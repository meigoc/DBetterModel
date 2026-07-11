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
    implementation(project(":api"))
    compileOnly("io.papermc.paper:paper-api:${rootProject.property("craftbukkit.version")}")
    compileOnly("io.github.toxicity188:bettermodel-api:${rootProject.property("bettermodel.v2.version")}") {
        isTransitive = false
    }
    compileOnly("io.github.toxicity188:bettermodel-bukkit-api:${rootProject.property("bettermodel.v2.version")}") {
        isTransitive = false
    }
}
