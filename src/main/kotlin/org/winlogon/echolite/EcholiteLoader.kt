package org.winlogon.echolite

import io.papermc.paper.plugin.loader.PluginClasspathBuilder
import io.papermc.paper.plugin.loader.PluginLoader
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository

class EcholiteLoader : PluginLoader {
    override fun classloader(classpathBuilder: PluginClasspathBuilder) {
        val jdaVersion = "6.1.3" // Consistent with build.gradle.kts
        val mcDiscordReserializerVersion = "4.4.0" // Consistent with build.gradle.kts

        val resolver = MavenLibraryResolver()

        resolver.addRepository(
            RemoteRepository.Builder(
                "central",
                "default",
                MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
            ).build()
        )

        // No need for scala3-library dependency since we are porting to Kotlin

        resolver.addDependency(
            Dependency(
                DefaultArtifact("net.dv8tion:JDA:$jdaVersion"),
                null
            )
        )

        resolver.addDependency(
            Dependency(
                DefaultArtifact("dev.vankka:mcdiscordreserializer:$mcDiscordReserializerVersion"),
                null
            )
        )

        classpathBuilder.addLibrary(resolver)
    }
}

