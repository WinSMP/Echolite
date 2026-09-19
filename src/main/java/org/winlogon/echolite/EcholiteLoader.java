// SPDX-License-Identifier: GPL-3.0-only
package org.winlogon.echolite;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.List;
import java.util.Map;

public class EcholiteLoader implements PluginLoader {

    private static final List<String> DEPENDENCIES = List.of(
        "net.dv8tion:JDA:6.6.0",
        "dev.vankka:mcdiscordreserializer:4.4.0",
        "org.winlogon:asynccraftr:0.2.0"
    );

    private static final Map<String, String> REPOSITORIES = Map.ofEntries(
        Map.entry("central", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR),
        Map.entry("winlogon", "https://maven.winlogon.org/releases")
    );

    public void classloader(PluginClasspathBuilder classpathBuilder) {
        var resolver = new MavenLibraryResolver();

        for (var kv : REPOSITORIES.entrySet()) {
            resolver.addRepository(
                new RemoteRepository.Builder(kv.getKey(), "default", kv.getValue()).build()
            );
        }

        for (var coord : DEPENDENCIES) {
            resolver.addDependency(new Dependency(new DefaultArtifact(coord), null));
        }

        classpathBuilder.addLibrary(resolver);
    }
}
