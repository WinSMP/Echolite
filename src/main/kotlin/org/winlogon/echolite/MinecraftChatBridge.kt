// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent.QuitReason
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.EventPriority

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import java.util.regex.Pattern

class MinecraftChatBridge(val config: Configuration, val discordBotManager: DiscordBotManager) : Listener {

    private fun String.getDiscordCompatible(): String {
        return if (isEmpty() || !contains('_')) {
            this
        } else {
            // (?<!_)_(?<!_) replaces single underscores not flanked by other underscores
            // Use replaceAll instead of replaceAllIn
            this.replace(Regex("(?<!_)_(?!_)"), "\\\\_") // Corrected escape
        }
    }

    private val plainTextSerializer = PlainTextComponentSerializer.plainText()

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerChat(event: AsyncChatEvent) {
        if (event.isCancelled) return

        val msg = plainTextSerializer.serialize(event.message())

        // equivalent to "&[a-zA-Z0-9]".r replaceAllIn (msg, "")
        val playerMessage = msg.replace(Regex("&[a-zA-Z0-9]"), "")
        // equivalent to """<[^>]*>""".r replaceAllIn (playerMessage, "")
        val miniMessageFormat = playerMessage.replace(Regex("<[^>]*>"), "")
        val message = config.minecraftMessage
            .replace("\$user_name", event.player.name)
            .replace("\$message", miniMessageFormat.trim())
        discordBotManager.sendMessageToDiscord(message)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (config.sendPlayerJoinMessages) {
            val player = event.player
            discordBotManager.sendMessageToDiscord("**${player.name.getDiscordCompatible()}** has joined the server!")
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!config.sendPlayerDeathMessages) {
            return
        }

        val deadPlayer = event.player
        val playerName = deadPlayer.name
        val minecraftDeathMessage: Component = event.deathMessage() ?: Component.text("died.")
        val discordMessage = plainTextSerializer.serialize(minecraftDeathMessage)
        val deathReason = discordMessage.replaceFirst("$playerName ", "")

        discordBotManager.sendMessageToDiscord("**${playerName.getDiscordCompatible()}** $deathReason")
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!config.sendPlayerJoinMessages) { // This should probably be sendPlayerQuitMessages if it exists
            return
        }

        val quitReason = event.reason
        val displayName = event.player.name
        val formattedPlayerName = "**${displayName.getDiscordCompatible()}**"
        val message = when (quitReason) {
            QuitReason.DISCONNECTED -> "has left the server!"
            QuitReason.TIMED_OUT -> "has been kicked due to an unexpected error."
            QuitReason.ERRONEOUS_STATE -> "has been timed out."
            QuitReason.KICKED -> "has been kicked."
        }
        discordBotManager.sendMessageToDiscord("$formattedPlayerName $message")
    }
}
