import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    id("io.freefair.lombok") version "8.6"
    kotlin("jvm") version "1.9.24"
    `maven-publish`
}

group = "com.github.dedinc"
version = "1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

dependencies {
    // HTTP Client
    api("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON Processing
    api("com.fasterxml.jackson.core:jackson-databind:2.19.1")
    api("com.fasterxml.jackson.core:jackson-core:2.19.1")
    api("com.fasterxml.jackson.core:jackson-annotations:2.19.1")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.1")

    // Retry Logic (Tenacity equivalent for Java)
    api("io.github.resilience4j:resilience4j-retry:1.7.1")
    api("io.github.resilience4j:resilience4j-core:1.7.1")

    // Utilities
    api("commons-codec:commons-codec:1.18.0")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")

}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    // Standard library JAR without main class
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.dedinc"
            artifactId = "oprouter4j"
            version = "1.0"

            from(components["java"])

            // Configure POM for proper transitive dependency resolution
            pom {
                name.set("oprouter4j")
                description.set("A Java library for the OpenRouter API Client SDK")
                url.set("https://github.com/DedInc/oprouter4j")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("dedinc")
                        name.set("DedInc")
                        email.set("visitanimation@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/DedInc/oprouter4j.git")
                    developerConnection.set("scm:git:ssh://github.com:DedInc/oprouter4j.git")
                    url.set("https://github.com/DedInc/oprouter4j/tree/main")
                }
            }
        }
    }
}
