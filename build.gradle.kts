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
version = "0.0.1-SNAPSHOT"

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
    implementation("com.google.inject:guice:4.2.3")
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
        "com.jetbrains.sh",
        terminal
    )
    setPlugins(*plugins.toTypedArray())
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("""
      Add change notes here.<br>
      <em>most HTML tags may be used</em>""")
}