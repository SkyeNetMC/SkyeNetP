plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "me.pilkeysek"
version = "2.1.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:9.7.0")
}

val targetJavaVersion = 21
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand(project.properties)
        }
    }
    runServer {
        minecraftVersion("1.21.10")
    }
    compileJava {
        options.compilerArgs.add("-Xlint:deprecation")
    }
    shadowJar {
        archiveClassifier.set("")
        relocate("dev.jorel.commandapi", "me.pilkeysek.skyeNetP.commandapi")
    }
    build {
        dependsOn(shadowJar)
    }
}
