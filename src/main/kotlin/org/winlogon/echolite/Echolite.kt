// SPDX-License-Identifier: GPL-3.0-only
package org.winlogon.echolite

import net.dv8tion.jda.api.JDA

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.asynccraftr.AsyncCraftr

import java.util.UUID
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class Echolite : JavaPlugin(), Listener {
    private var jda: JDA? = null
    private lateinit var config: Configuration
    private lateinit var discordBotManager: DiscordBotManager
    val replyTargets: ConcurrentHashMap<UUID, Pair<String, String>> = ConcurrentHashMap()

    private fun loadConfig(): Configuration {
        // Ensure config file exists, create from resources if not
        saveDefaultConfig()

        // -- Merge config defaults with configured config only where values are missing --
        val pluginConfig = getConfig()

        val configStream = getResource("config.yml")
        checkNotNull(configStream) { "config.yml not found in JAR resources" }

        val defaults = YamlConfiguration.loadConfiguration(InputStreamReader(configStream, StandardCharsets.UTF_8))
        pluginConfig.setDefaults(defaults)
        pluginConfig.addDefaults(defaults)

        // -- Save the merged config to disk --
        saveConfig()

        return Configuration(
            getConfig().getString("discord.token") ?: "",
            getConfig().getString("discord.channel-id") ?: "",
            getConfig().getString("discord.discord-default-role") ?: "member",
            getConfig().getBoolean("discord.status-messages", false),
            getConfig().getBoolean("discord.player-join-messages", false),
            getConfig().getBoolean("discord.player-death-messages", false),
            getConfig().getStringList("discord.status-list").toList(),
            getConfig().getString("message.discord") ?: "",
            getConfig().getString("message.minecraft") ?: ""
        )
    }

    private fun validateConfig(config: Configuration): Boolean {
        logger.info("Validating configuration file...")
        if (
            config.token.isBlank() || config.channelId.isBlank() ||
            config.channelId == "CHANNEL_ID" || config.token == "BOT_TOKEN"
        ) {
            getLogger().severe(
                "The Discord bot isn't configured for use in this server. Check the config file."
            )
            return false
        } else {
            return true
        }
    }

    override fun onEnable() {
        config = loadConfig() // Initialize config object

        if (!validateConfig(config)) {
            server.pluginManager.disablePlugin(this)
            return
        }

        logger.info("Hello World! I'm running on ${if (AsyncCraftr.isRunningOnFolia()) "Folia" else "Bukkit"}")

        val state = ServerState(logger, config, Bukkit.getPluginManager(), this)
        discordBotManager = DiscordBotManager(state, replyTargets)
        discordBotManager.startBot()

        server.pluginManager.registerEvents(MinecraftChatBridge(config, discordBotManager), this)
        ReplyCommand.createAndRegisterCommand(this, discordBotManager, this.replyTargets)
    }

    override fun onDisable() {
        discordBotManager.shutdownBot()
    }
}
