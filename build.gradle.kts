import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "9.3.0"
}

group = "org.winlogon.echolite"
version = "0.4.1-SNAPSHOT"

val pluginName = "Echolite"
val pluginVersion = version.toString()
val basePackage = group.toString()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("net.dv8tion:JDA:6.1.3")
    compileOnly("dev.vankka:mcdiscordreserializer:4.4.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.test {
    useJUnitPlatform()
}


tasks.processResources {
    notCompatibleWithConfigurationCache(
        "Uses resource filtering for plugin metadata"
    )
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    inputs.properties(
        mapOf(
            "NAME" to pluginName,
            "VERSION" to pluginVersion,
            "PACKAGE" to basePackage
        )
    )

    filesMatching("paper-plugin.yml") {
        filter(
            ReplaceTokens::class,
            "tokens" to mapOf(
                "NAME" to pluginName,
                "VERSION" to pluginVersion,
                "PACKAGE" to basePackage
            )
        )
    }
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "$basePackage.Echolite"
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    minimize()
    mergeServiceFiles()

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.txt")
    exclude("module-info.class")
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
