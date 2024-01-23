plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.fruiture.cor.ccs"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.10")
    testImplementation("io.kotest:kotest-assertions-core:5.6.2")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

    shadowJar {
        manifest {
            attributes(mapOf("Main-Class" to "de.fruiture.cor.ccs.AppKt"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}