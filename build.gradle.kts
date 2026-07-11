plugins {
    `java-library`
    `maven-publish`
}

allprojects {
    group = "meigo"
    version = rootProject.findProperty("BUILD_NUMBER") as? String ?: "UNKNOWN"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://maven.citizensnpcs.co/repo")
    }
}

description = "DBetterModel"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

val bundled: Configuration by configurations.creating

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("craftbukkit.version")}")
    compileOnly("com.denizenscript:denizen:${property("denizen.version")}")
    implementation(project(":api"))

    bundled(project(":api"))
    bundled(project(":compat:v1"))
    bundled(project(":compat:v2"))
    bundled(project(":compat:v3"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    dependsOn(bundled)
    // The merged jar intentionally mixes classfile 65 (:core, :api, v1, v2) and 69 (v3):
    // compat.v3 classes are only classloaded on BM 3.x servers, which mandate Java 25.
    from({ bundled.filter { it.name.endsWith(".jar") }.map { zipTree(it) } })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("DBetterModel-${project.version}.jar")
    manifest {
        // Paper's plugin remapper scans every class in the jar and chokes on the
        // classfile-69 v3 classes when the server itself runs Java 21 (Paper <= 1.21.4).
        // Declaring mojang mappings skips the remap pass entirely — we never touch NMS.
        attributes("paperweight-mappings-namespace" to "mojang")
    }
}

// The compat boundary is enforced mechanically: :core must never touch BetterModel classes.
val importBan by tasks.registering {
    group = "verification"
    description = "Fails on kr.toxicity.model imports outside the compat layers"
    // Both :core and :api must stay BetterModel-free.
    val sources = fileTree("src/main/java") { include("**/*.java") } +
        fileTree("api/src/main/java") { include("**/*.java") }
    doLast {
        val violations = sources.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if (line.contains("kr.toxicity.model")) "${file.path}:${index + 1}" else null
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "importBan: ${violations.size} direct BetterModel reference(s) in :core:\n  " +
                    violations.joinToString("\n  ")
            )
        }
    }
}

tasks.check {
    dependsOn(importBan)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(mapOf("version" to version))
    }
}
