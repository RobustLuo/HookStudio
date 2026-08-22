plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "dev.hookstudio"
version = "0.1.0"

val hookStudioVersion = file("VERSION").readText().trim()

application {
    mainClass.set("dev.hookstudio.MainKt")
}

tasks.processResources {
    filesMatching("version.txt") {
        expand(mapOf("version" to hookStudioVersion))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}
