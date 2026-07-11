plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${rootProject.property("craftbukkit.version")}")
}

// The version-neutral API must never touch BetterModel classes — that is the whole point of the layer.
val importBan by tasks.registering {
    group = "verification"
    description = "Fails if the version-neutral API references BetterModel classes"
    val sources = fileTree("src/main/java") { include("**/*.java") }
    doLast {
        val violations = sources.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if (line.contains("kr.toxicity.model")) "${file.path}:${index + 1}" else null
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "importBan: :api must stay BetterModel-free:\n  " + violations.joinToString("\n  ")
            )
        }
    }
}

tasks.check {
    dependsOn(importBan)
}
