import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
    application
}
group = "ru.senin.kotlin.wiki"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.apurebase:arkenv:3.1.0")
    implementation("org.apache.commons:commons-compress:1.20")
    implementation(kotlin("reflect"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.3.1")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.0.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        outputs.upToDateWhen {false}
        showStandardStreams = true
    }
}

kotlin.sourceSets.all {
    languageSettings.apply {
        optIn("kotlin.time.ExperimentalTime")
    }
}