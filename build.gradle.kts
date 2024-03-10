import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"

    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jetbrains.dokka") version "1.9.10"

    `maven-publish`
    signing
    distribution
}

fun KotlinJvmTarget.registerShadowJar(mainClassName: String) {
    val targetName = name
    compilations.named("main") {
        tasks {
            val shadowJar = register<ShadowJar>("${targetName}ShadowJar") {
                group = "build"
                from(output)
                configurations = listOf(runtimeDependencyFiles)
                archiveAppendix.set(targetName)
                archiveClassifier.set("all")
                manifest {
                    attributes("Main-Class" to mainClassName)
                }
                mergeServiceFiles()
            }
            getByName("${targetName}Jar") {
                finalizedBy(shadowJar)
            }
        }
    }
}

val ossrhUsername: String? = System.getenv("OSSRH_USERNAME") ?: findProperty("ossrhUsername")?.toString()
val ossrhPassword: String? = System.getenv("OSSRH_PASSWORD") ?: findProperty("ossrhPassword")?.toString()

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        withSourcesJar(publish = true)

        registerShadowJar("MainKt")

        compilations.named("main") {
            tasks {
                create<Jar>("javadocJar") {
                    group = JavaBasePlugin.DOCUMENTATION_GROUP
                    archiveClassifier = "javadoc"
                    from(dokkaHtml)
                }
            }
        }
    }
    jvmToolchain(21)

    macosArm64 { binaries { executable() } }
    macosX64 { binaries { executable() } }
    linuxX64 { binaries { executable() } }
    // clikt has no linux arm 64 variant available :-(
    // see https://github.com/ajalt/clikt/issues/404
    // linuxArm64 { binaries { executable() } }
    mingwX64 { binaries { executable() } }

    sourceSets {
        val jvmTest by getting {
            dependencies {
                // the jvm warning for the dynamic agent must simply be accepted
                implementation("io.mockk:mockk:1.13.9")
            }
        }
    }
}

artifacts {
    archives(tasks["javadocJar"])
    archives(tasks["jvmSourcesJar"])
}

dependencies {
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    commonMainImplementation("com.github.ajalt.clikt:clikt:4.2.2")
    commonMainImplementation("com.kgit2:kommand:2.0.1")

    // getting JUnit 5 to work is a nightmare, so we just use kotlin test
    commonTestImplementation(kotlin("test"))
    commonTestImplementation("io.kotest:kotest-assertions-core:5.6.2")
}

publishing {
    repositories {
        if (ossrhUsername != null && ossrhPassword != null) {
            maven {
                val nexusUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                val nexusUrlSnapshots = uri("https://oss.sonatype.org/content/repositories/snapshots")

                credentials {
                    this.username = ossrhUsername
                    this.password = ossrhPassword
                }

                url = if (version.toString().endsWith("SNAPSHOT")) nexusUrlSnapshots
                else nexusUrl
            }
        }
    }
    publications {
        withType<MavenPublication> {
            val githubRepo = "fruiture/git-ccs"

            groupId = project.group.toString()
            version = project.version.toString()

            artifact(tasks["jvmShadowJar"])
            artifact(tasks["javadocJar"])

            pom {
                name = project.name
                description = project.description
                url = "https://github.com/${githubRepo}"
                packaging = "jar"

                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/license/mit/"
                    }
                }
                developers {
                    developer {
                        id = "richard-wallintin"
                        name = "Richard Wallintin"
                    }
                }
                scm {
                    url = "https://github.com/${githubRepo}.git"
                    connection = "scm:git:git://github.com/${githubRepo}.git"
                    developerConnection = "scm:git:git://github.com/${githubRepo}.git"
                }
                issueManagement {
                    url = "https://github.com/${githubRepo}/issues"
                }
            }
        }
    }
}

signing {
    if (System.getenv("GPG_PRIVATE_KEY") != null) {
        useInMemoryPgpKeys(
            System.getenv("GPG_PRIVATE_KEY"),
            System.getenv("GPG_PRIVATE_KEY_PASSWORD")
        )
    }
    sign(publishing.publications)
}

distributions {
    listOf(
        "linux-x64" to "linkReleaseExecutableLinuxX64",
        "macos-arm64" to "linkReleaseExecutableMacosArm64",
        "macos-x64" to "linkReleaseExecutableMacosX64",
        "windows-x64" to "linkReleaseExecutableMingwX64"
    ).map { (distName, linkTask) ->
        create(distName) {
            contents {
                from(tasks[linkTask])
                rename {
                    it.removeSuffix(".kexe")
                }
            }
        }
    }
}

// final "fixes" to the tasks that are generated above
tasks {
    // fix: make sure to publish after signing
    withType(AbstractPublishToMaven::class).configureEach {
        val signingTasks = withType<Sign>()
        mustRunAfter(signingTasks)
    }

    // fix: compress tar archives
    withType<Tar> {
        compression = Compression.GZIP
        archiveExtension = "tar.gz"
    }

    // fix: build all distributions
    build {
        val distArchiveTasks = withType<AbstractArchiveTask>().filter {
            it.name.contains("Dist")
        }
        dependsOn(distArchiveTasks)
    }
}