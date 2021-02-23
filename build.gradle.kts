plugins {
    id("org.jetbrains.intellij") version "0.6.5"
    java
    kotlin("jvm") version "1.4.21"
    id("io.franzbecker.gradle-lombok") version "2.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

group = "dev.nocalhost"

val git4idea = "git4idea"
val terminal = "terminal"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))


    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.auth0:java-jwt:3.12.0")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.google.guava:guava:27.1-jre")
    implementation("org.yaml:snakeyaml:1.27")


    annotationProcessor("org.projectlombok:lombok:1.18.16")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.16")

    testImplementation("junit", "junit", "4.12")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2020.3"
    val plugins = mutableListOf(
        git4idea,
        terminal
    )
    setPlugins(*plugins.toTypedArray())
    pluginName = "nocalhost-intellij-plugin"
    updateSinceUntilBuild = false
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes(
        """
      Fix: On MacOS nhctl not found <br />
      Support nocalhost v0.2.x. <br />
      First Upload. <br />
      """
    )
}

tasks.getByName<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>("buildSearchableOptions") {
    this.enabled = false
}