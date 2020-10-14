plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    jcenter()
    maven("https://jitpack.io")
}

val dittNavDependenciesVersion = "1.3"

dependencies {
    implementation("no.nav.personbruker.dittnav:dependencies:$dittNavDependenciesVersion")
}
