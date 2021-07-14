plugins {
    id("org.jetbrains.intellij") version "0.7.2"
    java
    kotlin("jvm") version "1.4.21"
    id("io.franzbecker.gradle-lombok") version "2.1"
    id("net.saliman.properties") version "1.5.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

group = "dev.nocalhost"

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

    implementation("com.github.zafarkhaja:java-semver:0.9.0")
    implementation("io.sentry:sentry:1.7.23") {
        exclude("org.slf4j")
    }

    implementation("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.6") {
        exclude("org.slf4j")
    }

    annotationProcessor("org.projectlombok:lombok:1.18.16")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.16")

    testImplementation("junit", "junit", "4.12")
}

var baseIDE = "IC"
if (project.hasProperty("baseIDE")) {
    baseIDE = project.property("baseIDE") as String
}
val platformVersion = prop("platformVersion").toInt()
val ideaVersion = prop("ideaVersion")
val nocalhostVersion = prop("version")

val git4ideaPlugin = "git4idea"
val terminalPlugin = "terminal"
val javaPlugin = "com.intellij.java"
val goPlugin = "org.jetbrains.plugins.go:" + when (platformVersion) {
    203 -> "203.5981.114"
    211 -> "211.6693.111"
    else -> error("Unexpected platform version: `$platformVersion`")
}

version = "$nocalhostVersion-" + when (platformVersion) {
    203 -> "2020.3"
    211 -> "2021.1"
    else -> error("Unexpected platform version: `$platformVersion`")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = ideaVersion
    val plugins = mutableListOf(
        git4ideaPlugin,
        terminalPlugin,
        javaPlugin,
        goPlugin
    )
    setPlugins(*plugins.toTypedArray())
    pluginName = "nocalhost-intellij-plugin"
    updateSinceUntilBuild = true
}

sourceSets {
    main {
        java.srcDirs("src/$platformVersion/main/java")
    }
}

tasks.runIde {
    if (baseIDE == "GO") {
        ideDirectory("/Applications/GoLand.app/Contents")
    }
}

tasks {
    patchPluginXml {
        pluginId("dev.nocalhost.nocalhost-intellij-plugin")
        pluginDescription(
            """
            <html>
                <p>
                    Nocalhost Intellij Plugin
                </p>
    
                <p>
                    Nocalhost is Cloud Native Development Environment.
                </p>
    
                <p>
                    Features:
                    <ul>
                        <li>Login to nocalhost API Server and list the DevSpaces</li>
                        <li>Install and Uninstall DevSpaces</li>
                        <li>Start DevMode to develop services</li>
                    </ul>
                </p>
    
                <p>
                Refer to <a href="https://nocalhost.dev/">nocalhost.dev</a> for more Nocalhost information.
                </p>
            </html>
            """.trimIndent()
        )
        changeNotes(
            """
          <a href="https://github.com/nocalhost/nocalhost/tree/main/CHANGELOG">https://github.com/nocalhost/nocalhost/tree/main/CHANGELOG</a>
          """
        )
    }

    publishPlugin {
        token(System.getenv("JETBRAINS_TOKEN"))
    }

    buildSearchableOptions {
        enabled = false
    }
}

fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties")