// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

import net.dv8tion.jda.api.JDA

import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class Echolite : JavaPlugin(), Listener {
    private var jda: JDA? = null
    private lateinit var config: Configuration
    private lateinit var discordBotManager: DiscordBotManager
    val replyTargets: ConcurrentHashMap<UUID, Pair<String, String>> = ConcurrentHashMap() // Reverted to val

    val pluginLogger: Logger = getLogger()

    fun isFolia(): Boolean {
        return try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private fun loadConfig(): Configuration {
        // Ensure config file exists, create from resources if not
        saveDefaultConfig()
        getConfig().options().copyDefaults(true) // Access JavaPlugin's config options
        saveConfig()

        return Configuration(
            getConfig().getString("discord.token") ?: "",
            getConfig().getString("discord.channel-id") ?: "",
            getConfig().getString("discord.discord-default-role") ?: "member", // Fixed typo: discord.default-role to discord.discord-default-role
            getConfig().getBoolean("discord.status-messages", false),
            getConfig().getBoolean("discord.player-join-messages", false),
            getConfig().getBoolean("discord.player-death-messages", false),
            getConfig().getStringList("discord.status-list")?.toList() ?: emptyList(),
            getConfig().getString("message.discord") ?: "",
            getConfig().getString("message.minecraft") ?: ""
        )
    }

    private fun validateConfig(config: Configuration): Boolean {
        pluginLogger.info("Validating configuration file...")
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

        pluginLogger.info("Hello World! I'm running on ${if (isFolia()) "Folia" else "Bukkit"}")

        val state = ServerState(logger, config, Bukkit.getPluginManager(), this)
        discordBotManager = DiscordBotManager(state, replyTargets)
        discordBotManager.startBot()

        server.pluginManager.registerEvents(MinecraftChatBridge(config, discordBotManager), this) // Pass the correct config object
        ReplyCommand.createAndRegisterCommand(this, discordBotManager, this.replyTargets)
    }

    override fun onDisable() {
        discordBotManager.shutdownBot()
    }
}
