plugins {
    id("org.jetbrains.intellij") version "0.6.5"
    java
    kotlin("jvm") version "1.4.21"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

group = "dev.nocalhost"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("junit", "junit", "4.12")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2020.3"
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("""
      Add change notes here.<br>
      <em>most HTML tags may be used</em>""")
}