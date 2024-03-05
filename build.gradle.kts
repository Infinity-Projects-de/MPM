import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm") version "1.9.21"
    id("xyz.jpenilla.run-paper") version "2.0.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.0.3"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.3"
    id("io.papermc.paperweight.userdev") version "1.5.3"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "de.infinityprojects"
version = "1.0-SNAPSHOT"
val mcVersion = "1.20.4"
val javaVersion = 17

bukkit {
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "de.infinityprojects.mpm.Main"
    authors = listOf("Chechu")
    website = "https://chechu.dev"
    apiVersion = "1.20"
    libraries =
        listOf(
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0",
            "org.jetbrains.kotlin:kotlin-reflect:1.9.22",
            "org.reflections:reflections:0.10.2",
        )

    commands {
        register("mpm") {
            permission = "mpm.admin"
        }
    }

    permissions {
        register("mpm.admin")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("$mcVersion-R0.1-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.0.0")

    implementation(kotlin("reflect"))

    compileOnly("org.reflections:reflections:0.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation(kotlin("test"))
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    build {
        dependsOn(ktlintFormat) // TODO does it work?
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(javaVersion)
    }

    shadowJar {
        relocate("org.bstats", "de.infinityprojects.mpm.bstats")
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    runServer {
        minecraftVersion(mcVersion)
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }

    test {
        useJUnitPlatform()
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(javaVersion)
}
