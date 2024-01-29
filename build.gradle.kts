import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm").version(Kotlin.version)
    kotlin("plugin.allopen").version(Kotlin.version)

    id(Shadow.pluginId) version (Shadow.version)

    // Apply the application plugin to add support for building a CLI application.
    application
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}


repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven("https://maven.pkg.github.com/navikt/*") {
        credentials {
            username = System.getenv("GITHUB_ACTOR")?: "x-access-token"
            password = System.getenv("GITHUB_TOKEN")?: project.findProperty("githubPassword") as String
        }
    }
    mavenLocal()
}

sourceSets {
    create("intTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

dependencies {
    implementation(Doknotifikasjon.schemas)
    implementation(Kafka.clients)
    implementation(Avro.avroSerializer)
    implementation(Logstash.logbackEncoder)
    implementation(KotlinLogging.logging)
    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(TmsCommonLib.utils)
    implementation(TmsCommonLib.observability)
    implementation(RapidsAndRivers.rapidsAndRivers)
    implementation(JacksonDataType14.moduleKotlin)

    testImplementation(Junit.api)
    testImplementation(Junit.engine)
    testImplementation(Junit.params)
    testImplementation(Kafka.kafka_2_12)
    testImplementation(Kafka.streams)
    testImplementation(Mockk.mockk)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
}

application {
    mainClass.set("no.nav.tms.ekstern.varselbestiller.ApplicationKt")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }
}

// TODO: Fjern følgende work around i ny versjon av Shadow-pluginet:
// Skal være løst i denne: https://github.com/johnrengelman/shadow/pull/612
project.setProperty("mainClassName", application.mainClass.get())
apply(plugin = Shadow.pluginId)
