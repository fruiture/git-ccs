plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
    signing
}

val ossrhUsername: String by project
val ossrhPassword: String by project

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.10")
    testImplementation("io.kotest:kotest-assertions-core:5.6.2")
    testImplementation("io.mockk:mockk:1.13.9")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    test {
        jvmArgs("-XX:+EnableDynamicAgentLoading")
    }

    shadowJar {
        archiveClassifier = "all"
        manifest {
            attributes(mapOf("Main-Class" to "de.fruiture.cor.ccs.CLIKt"))
        }
    }

    build {
        dependsOn(shadowJar)
    }

    create<Jar>("javadocJar") {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        archiveClassifier = "javadoc"
        from(javadoc)
    }

}

artifacts {
    archives(tasks["javadocJar"])
    archives(tasks.kotlinSourcesJar)
}

publishing {
    repositories {
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
    publications {
        create<MavenPublication>("maven") {
            val githubRepo = "fruiture/git-ccs"

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["kotlin"])
            // could not find a more elegant solution here...
            artifact(tasks.shadowJar)
            artifact(tasks.kotlinSourcesJar)
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
    sign(publishing.publications["maven"])
}
