/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java application project to get you started.
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/7.0.2/userguide/building_java_projects.html
 */

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    jacoco
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit test framework.
    testImplementation("junit:junit:4.13.1")

    // This dependency is used by the application.
    implementation("com.google.guava:guava:30.0-jre")

    // Test Coverage
    // implementation("org.jacoco:jacoco-maven-plugin:0.8.7")
}

application {
    // Define the main class for the application.
    mainClass.set("de.nox.liquiddemocracy.Main")
}


// create a jar.
tasks.withType<Jar>() {
  manifest {
    attributes["Main-Class"] = "de.nox.liquiddemocracy.Main"
  }
}

// // jacoco: test coverage, configuration: ALWAYS in tests
tasks.test {
  finalizedBy(tasks.jacocoTestReport) // generate always on test runs
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
