plugins {
    id("org.jetbrains.intellij.platform") version "2.9.0"
    java
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.lombok") version "1.9.22"
    id("io.franzbecker.gradle-lombok") version "2.1"
    id("net.saliman.properties") version "1.5.1"
}

java {
    sourceCompatibility = JavaVersion.valueOf(prop("javaCompatibility"))
    targetCompatibility = JavaVersion.valueOf(prop("javaCompatibility"))
}

group = "dev.nocalhost"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.auth0:java-jwt:3.12.0")
    implementation("com.google.code.gson:gson:2.8.9")
//  If import guava, NocalhostNodeConfiguration#createDebugProcess will report the following error at runtime, `com.jetbrains:ideaIU` has built-in guava, so remove the dependency here
//  Error running 'Nocalhost': loader constraint violation: loader com.intellij.ide.plugins.cl.PluginClassLoader @690f178e (instance of com.intellij.ide.plugins.cl.PluginClassLoader, child of 'bootstrap') wants to load interface com.google.common.collect.BiMap. A different interface with the same name was previously loaded by com.intellij.ide.plugins.cl.PluginClassLoader @1acb7e07 (instance of com.intellij.ide.plugins.cl.PluginClassLoader, child of 'bootstrap').
//  implementation("com.google.guava:guava:27.1-jre")
    implementation("org.yaml:snakeyaml:1.27")

    implementation("com.github.zafarkhaja:java-semver:0.9.0")
    implementation("io.sentry:sentry:1.7.23") {
        exclude("org.slf4j")
    }

    implementation("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.6") {
        exclude("org.slf4j")
    }

    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")

    testImplementation("junit", "junit", "4.12")
}

var baseIDE = "IU"
if (project.hasProperty("baseIDE")) {
    baseIDE = project.property("baseIDE") as String
}
val platformVersion = prop("platformVersion").toInt()
val ideaVersion = prop("ideaVersion")
val nocalhostVersion = prop("version")
val changelogVersion = nocalhostVersion.replace("(\\d+\\.\\d+\\.)(\\d+)".toRegex(), "$1x")

val terminalPlugin = "org.jetbrains.plugins.terminal"
var javascriptPlugin = "JavaScript"
var javascriptDebuggerPlugin = "JavaScriptDebugger"
val javaPlugin = "com.intellij.java"

version = "$nocalhostVersion-$platformVersion"

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellijPlatform {
    pluginConfiguration {
        name = "nocalhost-intellij-plugin"
    }
//    updateSinceUntilBuild.set(true)
}
dependencies {
    intellijPlatform {
        intellijIdeaUltimate(ideaVersion)
        compatiblePlugins(
            "Pythonid",
            "com.jetbrains.php",
            "org.jetbrains.plugins.go",
        )
        if (!ideaVersion.startsWith("2024.1")) {
            compatiblePlugins("PythonCore") // PythonCore new from 2024.2
        }
        bundledPlugins(listOf(
            javascriptDebuggerPlugin,
            javascriptPlugin,
            javaPlugin,
            terminalPlugin,
            "Git4Idea"
        ))
    }
}

sourceSets {
    main {
        java.srcDirs("src/$platformVersion/main/java")
    }
}

//Install other ide at your local and config these paths if you want to run other ide: RunGoland and so on
/*
tasks.runIde {
    if (baseIDE == "IC") {
        ideDir.set(File("/Applications/IntelliJ IDEA CE.app/Contents"))
    }
    if (baseIDE == "GO") {
        ideDir.set(File("/Applications/GoLand.app/Contents"))
    }
    if (baseIDE == "Node") {
        ideDir.set(File("/Applications/WebStorm.app/Contents"))
    }
    if (baseIDE == "Python") {
        ideDir.set(File("/Applications/PyCharm.app/Contents"))
    }
    if (baseIDE == "PHP") {
        ideDir.set(File("/Applications/PhpStorm.app/Contents"))
    }
}
*/

tasks {
    patchPluginXml {
        pluginId.set("dev.nocalhost.nocalhost-intellij-plugin")
        pluginDescription.set(provider { file("description.html").readText() })
        changeNotes.set(
            """
            <h2>Version $nocalhostVersion</h2>
            <p>
                <a href="https://nocalhost.dev/docs/changelogs/$changelogVersion/">https://nocalhost.dev/docs/changelogs/$changelogVersion</a>
            </p>
            """
        );
    }

    publishPlugin {
        token.set(System.getenv("JETBRAINS_TOKEN"))
    }

    buildSearchableOptions {
        enabled = false
    }
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
}

fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties")
