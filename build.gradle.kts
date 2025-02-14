import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    `maven-publish`
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.tbr.test"
version = "0.0.1-SNAPSHOT"


// Disable the default bootJar task
// Since we use only spring boot dependencies management
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")

}

tasks.withType<Test> {
    useJUnitPlatform()
}
