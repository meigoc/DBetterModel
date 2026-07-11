plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "DBetterModel"

include(":api", ":compat:v1", ":compat:v2", ":compat:v3")
