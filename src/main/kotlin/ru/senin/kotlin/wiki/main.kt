package ru.senin.kotlin.wiki

import com.apurebase.arkenv.Arkenv
import com.apurebase.arkenv.argument
import com.apurebase.arkenv.parse
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.channels.Channels
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.measureTime


class Parameters : Arkenv() {
    val inputs: List<File>? by argument("--inputs") {
        description = "Path(s) to bzip2 archived XML file(s) with WikiMedia dump. Comma separated."
        mapping = {
            it.split(",").map{ name -> File(name) }
        }
        validate("File does not exist or cannot be read") {
            it.all { file -> file.exists() && file.isFile && file.canRead() }
        }
    }

    val output: String by argument("--output") {
        description = "Report output file"
        defaultValue = { "statistics.txt" }
    }

    val threads: Int by argument("--threads") {
        description = "Number of threads"
        defaultValue = { 4 }
        validate("Number of threads must be in 1..32") {
            it in 1..32
        }
    }

    val date: Int? by argument("--date") {
        description = "Date of dumps for bonus task"
    }
}

fun main(args: Array<String>) {
    try {
        val parameters = Parameters().parse(args)
        val inputsIfDate : List<File> = mutableListOf()
        val stats = Stats()
        val bufferSize = 32768

        if (parameters.help) {
            println(parameters.toString())
            return
        }

        if (parameters.date != null && parameters.inputs == null) {
            if (!File("wikiTest/").exists()){
                File("wikiTest/").mkdirs()
            }

            var siteUrl = URL("https://dumps.wikimedia.org/ruwiki/${parameters.date}/dumpstatus.json")
            var readChannel = Channels.newChannel(siteUrl.openStream())
            var channelOut = FileOutputStream("wikiTest/filesList.txt")

            channelOut.channel.transferFrom(readChannel, 0, Long.MAX_VALUE)

            val archivesRegex =
                Regex("""ruwiki-${parameters.date}-pages-meta-current1+\.xml-p[0-9]+p[0-9]+\.bz2""")
            val archivesList: MutableList<String> = mutableListOf()
            val filesListReader: BufferedReader = File("wikiTest/filesList.txt").bufferedReader()
            val filesListText = filesListReader.use { it.readText() }

            archivesRegex.findAll(filesListText.lowercase(Locale.getDefault())).toList().forEach {
                archivesList.add(it.value)
            }
            for (element in archivesList) {
                siteUrl = URL("https://dumps.wikimedia.org/ruwiki/${parameters.date}/$element")
                readChannel = Channels.newChannel(siteUrl.openStream())
                channelOut = FileOutputStream("wikiTest/$element")

                channelOut.channel.transferFrom(readChannel, 0, Long.MAX_VALUE)
                inputsIfDate.plus(channelOut)
            }
        }
        else if (parameters.inputs == null) {
            throw IOException("File does not exist")
        }

        val duration = measureTime {
            val filesInput : List<File> = inputsIfDate.ifEmpty {
                parameters.inputs!! // it was checked for null
            }

            if (parameters.threads > 1) {
                val threadPool = Executors.newFixedThreadPool(parameters.threads - 1)

                for (file in filesInput) {
                    threadPool.submit {
                        val fileStats = solve(file, bufferSize)

                        stats.merge(fileStats)
                    }
                }

                threadPool.shutdown()
                if (!threadPool.awaitTermination(30, TimeUnit.MINUTES)) {
                    threadPool.shutdownNow()
                }
            }
            else {
                for (file in filesInput) {
                    val fileStats = solve(file, bufferSize)

                    stats.merge(fileStats)
                }
            }
        }
        println("Time: ${duration.inWholeMilliseconds} ms")

        if (inputsIfDate.isEmpty()) {
            stats.print(parameters.output, false)
        }
        else {
            stats.print("statistics.html", true)
        }
    }
    catch (e: Exception) {
        println("Error! ${e.message}")
        throw e
    }
}