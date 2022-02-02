package ru.senin.kotlin.wiki

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import javax.xml.parsers.SAXParserFactory
import kotlin.collections.HashMap


open class SaxParserHandler : DefaultHandler() {
    private val titleTags: List<String> = listOf("mediawiki", "page", "title")
    private val textTags: List<String> = listOf("mediawiki", "page", "revision", "text")
    private val timeTags: List<String> = listOf("mediawiki", "page", "revision", "timestamp")
    private val currentTags: MutableList<String> = mutableListOf()
    private val russianWordsRegex = Regex("""[а-яА-Я]{3,}""")
    private val yearRegex = Regex("""[0-9]{4}-[0-9]{2}-[0-9]{2}""")

    private val stats = Stats()
    private var curWords : StringBuilder = StringBuilder()
    private var bytes : Int? = null

    private var curTitle: MutableList<String> = mutableListOf()
    private var curText: MutableList<String> = mutableListOf()
    private var curTime: Int = 0

    fun getStats(): Stats {
        return stats
    }

    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
        currentTags.add(qName ?: "")

        if (currentTags == textTags) {
            for (element in 0 until (attributes?.length ?: 0)) {
                if (attributes?.getLocalName(element) == "bytes") {
                    bytes = attributes.getValue(element).toInt()

                    break
                }
            }

            bytes ?: throw IllegalArgumentException("Not well-formed XML!")
        }

        when (currentTags) { titleTags, textTags, timeTags -> curWords = StringBuilder() }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        val russianWords = findRussianWords(curWords.toString())

        when (currentTags) {
            titleTags -> curTitle = russianWords
            textTags -> curText = russianWords
            timeTags -> curTime = findTime(curWords.toString())
        }

        if (qName == "page" && curTitle.isNotEmpty() && curText.isNotEmpty() && curTime != 0) {
            stats.statsUpdate(curTitle, curText, bytesToPos(), curTime)

            curTitle = mutableListOf()
            curText = mutableListOf()
            curTime = 0
        }

        currentTags.removeLast()
    }

    override fun characters(ch: CharArray?, start: Int, length: Int) {
        curWords.append(String(ch ?: CharArray(0), start, length))
    }

    private fun findRussianWords(text: String): MutableList<String> {
        val wordsList: MutableList<String> = mutableListOf()

        russianWordsRegex.findAll(text.lowercase(Locale.getDefault())).toList().forEach {
            wordsList.add(it.value)
        }

        return wordsList
    }

    private fun bytesToPos(): Int? {
        var pos = 0
        var pow = 10

        if (bytes == null) return null

        while (bytes!! >= pow) {
            pow *= 10
            pos++
        }
        bytes = null

        return pos
    }

    private fun findTime(text: String): Int {
        try {
            val year = yearRegex.find(text)?.value?.substring(0..3)?.toInt()

            return year ?: throw NullPointerException("Not well-formed XML!")
        } catch (e : Exception) {
            println("Not well-formed XML!")
            throw e
        }
    }
}

class Stats(val title: HashMap<String, Int> = HashMap(), val text: HashMap<String, Int> = HashMap(),
            val bytes: HashMap<Int, Int> = HashMap(), val time: HashMap<Int, Int> = HashMap()) {

    fun statsUpdate(titleText: MutableList<String> = mutableListOf(), textText: MutableList<String> = mutableListOf(),
                    bytesPos: Int? = null, timeText: Int = 0) {
        titleText.forEach { title[it] = title.getOrDefault(it, 0) + 1 }
        textText.forEach { text[it] = text.getOrDefault(it, 0) + 1 }
        if (bytesPos != null) bytes[bytesPos] = bytes.getOrDefault(bytesPos, 0) + 1
        if (timeText != 0) time[timeText] = time.getOrDefault(timeText, 0) + 1
    }

    @Synchronized fun merge(newStats: Stats) {
        mapMerge(newStats.title, title)
        mapMerge(newStats.text, text)
        mapMerge(newStats.bytes, bytes)
        mapMerge(newStats.time, time)
    }

    fun print(fileName: String, printHtml: Boolean) {
        val newTitle = title.entries.sortedWith(sortFun()).take(300)
        val newText = text.entries.sortedWith(sortFun()).take(300)
        val firstBytesPos = bytes.minByOrNull { it.key }?.key ?: 0
        val lastBytesPos = bytes.maxByOrNull { it.key }?.key ?: -1
        val firstYear = time.minByOrNull { it.key }?.key ?: 0
        val lastYear = time.maxByOrNull { it.key }?.key ?: -1
        val out = PrintWriter(OutputStreamWriter(FileOutputStream(fileName), StandardCharsets.UTF_8), true)

        if (printHtml) {
            var bytesSum = 0
            var timeSum = 0
            
            bytes.forEach { bytesSum += it.value }
            time.forEach { timeSum += it.value }

            out.println("<h1 style=\"text-align: center;\">Статистика архивов</h1>")

            out.print("<p>Топ-300 слов в заголовках статей:")
            newTitle.forEach { out.println("<br />${it.value} ${it.key}") }
            out.println("</p>")

            out.print("<p>Топ-300 слов в статьях:")
            newText.forEach { out.println("<br />${it.value} ${it.key}") }
            out.println("</p>")

            out.print("<p>Гистограмма распределения статей по размеру:")
            for (pos in firstBytesPos..lastBytesPos) {
                val cnt = bytes.getOrDefault(pos, 0)
                val percents = cnt * 100 / bytesSum

                out.print("<br />$pos: ")
                for (i in 0 until percents) {
                    out.print("=")
                }
                out.print("&gt;<br />")
            }
            out.println("</p>")

            out.print("<p>Гистограмма распределения статей по времени:")
            for (year in firstYear..lastYear) {
                val cnt = time.getOrDefault(year, 0)
                val percents = cnt * 100 / timeSum

                out.print("<br />$year: ")
                for (i in 0 until percents) {
                    out.print("=")
                }
                out.print("&gt;<br />")
            }
            out.println("</p>")
        }
        else {
            out.println("Топ-300 слов в заголовках статей:")
            newTitle.forEach { out.println("${it.value} ${it.key}") }
            out.println()

            out.println("Топ-300 слов в статьях:")
            newText.forEach { out.println("${it.value} ${it.key}") }
            out.println()

            out.println("Распределение статей по размеру:")
            for (pos in firstBytesPos..lastBytesPos) {
                out.println("$pos ${bytes.getOrDefault(pos, 0)}")
            }
            out.println()

            out.println("Распределение статей по времени:")
            for (year in firstYear..lastYear) { out.println("$year ${time.getOrDefault(year, 0)}") }
        }

        out.close()
    }

    private fun sortFun() = Comparator<MutableMap.MutableEntry<String, Int>> { first, second ->
        when {
            first.key == second.key && first.value == second.value -> 0
            first.value < second.value -> 1
            first.value == second.value && first.key > second.key -> 1
            else -> -1
        }
    }

    private fun <T> mapMerge(mapFrom: HashMap<T, Int>, mapTo: HashMap<T, Int>) {
        mapFrom.forEach { mapTo[it.key] = mapTo.getOrDefault(it.key, 0) + it.value }
    }
}

class WikiSaxParser {
    @Throws(Exception::class)

    fun parse(input: InputStream): Stats {
        val parser = SAXParserFactory.newInstance().newSAXParser()
        val handler = SaxParserHandler()

        parser.parse(input, handler)

        return handler.getStats()
    }
}

fun solve(input: File, buffer: Int): Stats {
    try {
        return WikiSaxParser().parse(BZip2CompressorInputStream(BufferedInputStream(input.inputStream(), buffer)))
    }
    catch (e: Exception) {
        println("Error! ${e.message}")
        throw e
    }
}