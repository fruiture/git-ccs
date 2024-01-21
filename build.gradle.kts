plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("multiplatform") version "1.2.21"
}

group = "de.fruiture.cor.ccs"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}