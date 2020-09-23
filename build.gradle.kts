import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val prometheusVersion = "0.6.0"
val ktorVersion = "1.3.0"
val junitVersion = "5.4.1"
val kafkaVersion = "2.3.0"
val confluentVersion = "5.3.0"
val brukernotifikasjonSchemaVersion = "1.2020.02.07-13.16-fa9d319688b1"
val logstashVersion = 5.2
val logbackVersion = "1.2.3"
val vaultJdbcVersion = "1.3.1"
val hikariCPVersion = "3.2.0"
val postgresVersion = "42.2.5"
val h2Version = "1.4.200"
val assertJVersion = "3.12.2"
val kafkaEmvededVersion = "2.2.1"
val kluentVersion = "1.52"
val kafkaEmbeddedEnvVersion = "2.1.1"
val mockkVersion = "1.9.3"
val influxdbVersion = "2.8"

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    val kotlinVersion = "1.3.50"
    kotlin("jvm").version(kotlinVersion)
    kotlin("plugin.allopen").version(kotlinVersion)

    // Apply the application plugin to add support for building a CLI application.
    application
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

repositories {
    jcenter()
    mavenCentral()
    maven("http://packages.confluent.io/maven")
    mavenLocal()
}

sourceSets {
    create("intTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

val intTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
configurations["intTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_logback:$prometheusVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("no.nav:brukernotifikasjon-schemas:$brukernotifikasjonSchemaVersion")
    implementation("org.influxdb:influxdb-java:$influxdbVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation(kotlin("test-junit5"))
    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion")
    testImplementation("org.apache.kafka:kafka_2.12:$kafkaVersion")
    testImplementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    testImplementation("io.confluent:kafka-schema-registry:$confluentVersion")
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    intTestImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

tasks {
    withType<Jar> {
        manifest {
            attributes["Main-Class"] = application.mainClassName
        }
        from(configurations.runtime.get().map { if (it.isDirectory) it else zipTree(it) })
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }

    register("runServer", JavaExec::class) {
        environment("KAFKA_BOOTSTRAP_SERVERS", "localhost:29092")
        environment("KAFKA_SCHEMAREGISTRY_SERVERS", "http://localhost:8081")
        environment("SERVICEUSER_USERNAME", "username")
        environment("SERVICEUSER_PASSWORD", "password")
        environment("GROUP_ID", "dittnav_varsel-bestiller")
        environment("DB_HOST", "localhost:5432")
        environment("DB_NAME", "dittnav-event-cache-preprod")
        environment("DB_PASSWORD", "testpassword")
        environment("DB_MOUNT_PATH", "notUsedOnLocalhost")
        environment("NAIS_CLUSTER_NAME", "dev-sbs")
        environment("NAIS_NAMESPACE", "q1")
        environment("SENSU_HOST", "stub")
        environment("SENSU_PORT", "")
        environment("PRODUCER_ALIASES", "")

        main = application.mainClassName
        classpath = sourceSets["main"].runtimeClasspath
    }
}

val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["intTest"].output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath
    shouldRunAfter("test")
}

tasks.check { dependsOn(integrationTest) }
