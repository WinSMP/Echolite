// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer

import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit // For Folia scheduler delays
import kotlin.collections.set // Explicitly import 'set' for map operations

class DiscordChatBridge(
    val plugin: JavaPlugin,
    val config: Configuration,
    val replyTargets: ConcurrentHashMap<UUID, Pair<String, String>>
) : ListenerAdapter() { // extends ListenerAdapter in Scala becomes : ListenerAdapter() in Kotlin
    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()
    private val minecraftSerializer = MinecraftSerializer.INSTANCE
    private val miniMessage = MiniMessage.miniMessage()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) {
            return
        }

        if (event.channel.id == config.channelId) {
            val roles = event.member?.roles
            val userRole = if (!roles.isNullOrEmpty()) roles[0].name else config.defaultRole

            val rawConfig = config.discordMessage
                .replace("\$display_name", event.author.effectiveName)
                .replace("\$handle", event.author.name)
                .replace("\$role", userRole)

            val discordMessageComponent: Component =
                minecraftSerializer.serialize(event.message.contentDisplay)
            val discordMessageLegacy = legacySerializer.serialize(discordMessageComponent)
            val finalLegacyMessage = rawConfig.replace("\$message", discordMessageLegacy)
            val finalComponent = legacySerializer.deserialize(finalLegacyMessage)

            if (!isFolia()) {
                object : BukkitRunnable() {
                    override fun run() {
                        Bukkit.broadcast(finalComponent)
                    }
                }.runTask(plugin)
            } else {
                // Folia's scheduler.execute takes a Runnable
                Bukkit.getGlobalRegionScheduler().execute(
                    plugin,
                    Runnable { Bukkit.broadcast(finalComponent) }
                )
            }
        } else if (event.isFromGuild) {
            // not a DM, and not in the right channel
            return
        } else {
            // This is a DM, check if it's a reply to a player
            val userId = event.author.id
            val message = event.message.contentRaw
            val targetEntry = replyTargets.entries.find { (_, value) -> value.first == userId }

            targetEntry?.let { (playerUUID, _) ->
                val player = Bukkit.getPlayer(playerUUID)
                if (player != null && player.isOnline) {
                    val nameComponent = Component.text(event.author.name, NamedTextColor.DARK_AQUA)
                    val messageComponent = Component.text(message, NamedTextColor.GRAY)
                    val richMessage = miniMessage.deserialize(
                        "<dark_gray>(<gray><sender> -> <dark_green>you</dark_green></gray>)</dark_gray> <message>",
                        Placeholder.component("sender", nameComponent),
                        Placeholder.component("message", messageComponent)
                    )
                    player.sendMessage(richMessage)
                }
            }
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val players = Bukkit.getOnlinePlayers()
        when (event.name) {
            "list" -> {
                val playerNames = if (players.isEmpty()) {
                    "No players are currently online."
                } else {
                    players.joinToString(", ") { it.name }
                }
                event
                    .reply("Online Players: $playerNames")
                    .setEphemeral(true)
                    .queue()
            }
            "msg" -> {
                val playerName = event.getOption("player")?.asString
                val message = event.getOption("message")?.asString
                val player = playerName?.let { Bukkit.getPlayer(it) }

                if (playerName != null && message != null) {
                    messageCommand(message, player, event)
                } else {
                    event
                        .reply("Invalid command usage. Player name or message is missing.")
                        .setEphemeral(true)
                        .queue()
                }
            }
            else -> {
                // Ignore unknown commands
            }
        }
    }

    private fun messageCommand(
        message: String,
        player: Player?, // Player can be null if not found
        event: SlashCommandInteractionEvent
    ) {
        val playerName = player?.name ?: event.getOption("player")?.asString ?: "Unknown Player"

        if (player == null || !player.isOnline) {
            event
                .reply("Player '$playerName' is not online or does not exist.")
                .setEphemeral(true)
                .queue()
            return
        }

        val user = event.user
        replyTargets[player.uniqueId] = Pair(user.id, user.name)

        val nameComponent = Component.text(event.user.name, NamedTextColor.DARK_AQUA)
        val messageComponent = Component.text(message, NamedTextColor.GRAY)
        val richMessage = miniMessage.deserialize(
            "<dark_gray>(<gray><sender> -> <dark_green>you</dark_green></gray>)</dark_gray> <message>",
            Placeholder.component("sender", nameComponent),
            Placeholder.component("message", messageComponent)
        )

        if (!isFolia()) {
            object : BukkitRunnable() {
                override fun run() {
                    player.sendMessage(richMessage)
                }
            }.runTask(plugin)
        } else {
            Bukkit.getGlobalRegionScheduler().execute(
                plugin,
                Runnable { player.sendMessage(richMessage) }
            )
        }

        event
            .reply("Message sent to $playerName!")
            .setEphemeral(true)
            .queue()
    }

    private fun isFolia(): Boolean {
        return try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
