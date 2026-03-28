plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("com.gradleup.shadow") version "9.0.0-beta12"
    id("com.gradleup.nmcp") version "1.4.4"
    id("com.diffplug.spotless") version "8.2.1"
}

group = "dev.demeng.sentinel"
version = findProperty("releaseVersion") as String? ?: "0.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    compileOnly("org.jspecify:jspecify:1.0.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.jspecify:jspecify:1.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.google.gson", "dev.demeng.sentinel.client.internal.gson")
    minimize()
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    java {
        googleJavaFormat()
        targetExclude("build/**")
    }
}

val setupGitHooks by tasks.registering(Exec::class) {
    commandLine("git", "config", "core.hooksPath", ".githooks")
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:none", "-quiet")
    }
    exclude("dev/demeng/sentinel/client/internal/**")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["shadow"])
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
            pom {
                name.set("Sentinel Java Client")
                description.set("Java client library for Sentinel")
                url.set("https://github.com/demengc/sentinel-java-client")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("demengc")
                        name.set("Demeng")
                        email.set("hi@demeng.dev")
                        url.set("https://github.com/demengc")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/demengc/sentinel-java-client.git")
                    developerConnection.set("scm:git:ssh://github.com/demengc/sentinel-java-client.git")
                    url.set("https://github.com/demengc/sentinel-java-client")
                }
            }
        }
    }
}

signing {
    val signingKey = findProperty("signingInMemoryKey") as String?
    val signingPassword = findProperty("signingInMemoryKeyPassword") as String?
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    sign(publishing.publications["mavenJava"])
}

tasks.withType<Sign>().configureEach {
    isRequired = !version.toString().endsWith("-SNAPSHOT")
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username = findProperty("centralPortalUsername") as String? ?: ""
        password = findProperty("centralPortalPassword") as String? ?: ""
        publishingType = "USER_MANAGED"
    }
}
