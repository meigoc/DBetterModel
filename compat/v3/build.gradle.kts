plugins {
    `java-library`
}

// BetterModel 3.x is published as classfile 69: this module needs JDK 25 (auto-provisioned
// via the foojay toolchain resolver). Its classes only ever load on BM 3.x servers,
// which mandate a Java 25 JVM — see ARCHITECTURE-6.0.md.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}

dependencies {
    implementation(project(":api"))
    compileOnly("io.papermc.paper:paper-api:${rootProject.property("craftbukkit.version")}")
    compileOnly("io.github.toxicity188:bettermodel-api:${rootProject.property("bettermodel.v3.version")}") {
        isTransitive = false
    }
    compileOnly("io.github.toxicity188:bettermodel-bukkit-api:${rootProject.property("bettermodel.v3.version")}") {
        isTransitive = false
    }
}
