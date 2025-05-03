plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://mvn.lumine.io/repository/maven-public/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }

    maven {
        url = uri("https://maven.citizensnpcs.co/repo")
    }
}

dependencies {
    implementation("io.papermc.paper:paper-api:${project.properties["craftbukkit.version"]}")
    implementation("com.denizenscript:denizen:${project.properties["denizen.version"]}")
    compileOnly("io.github.toxicity188:BetterModel:${project.properties["bettermodel.version"]}")
}

fun buildNumber(): String = project.findProperty("BUILD_NUMBER") as? String ?: "UNKNOWN"

group = "net.openproject"
version = buildNumber() + "-DEV"
description = "DBetterModel"

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(mapOf("BUILD_NUMBER" to System.getenv("BUILD_NUMBER")))
    }
}
