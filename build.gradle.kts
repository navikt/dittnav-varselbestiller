import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm").version(Kotlin.version)

    id(Shadow.pluginId) version (Shadow.version)

    application
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    mavenLocal()
}

dependencies {
    implementation(Doknotifikasjon.schemas)
    implementation(Kafka.clients)
    implementation(Avro.avroSerializer)
    implementation(KotlinLogging.logging)
    implementation(Logstash.logbackEncoder)
    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(TmsCommonLib.utils)
    implementation(TmsCommonLib.observability)
    implementation(TmsKafkaTools.kafkaApplication)
    implementation(JacksonDataType14.moduleKotlin)
    implementation(JacksonDatatype.datatypeJsr310)

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
