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

import scala.jdk.CollectionConverters._
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.UUID
import scala.collection.mutable
import scala.concurrent.ExecutionContext

class DiscordChatBridge(val plugin: JavaPlugin, val config: Configuration, val replyTargets: mutable.Map[java.util.UUID, (String, String)]) extends ListenerAdapter {
  private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()
  private val minecraftSerializer = MinecraftSerializer.INSTANCE
  private val miniMessage = MiniMessage.miniMessage()

    override def onMessageReceived(event: MessageReceivedEvent): Unit = {
        if (!event.getChannel.getId.equals(config.channelId) || event.getAuthor.isBot) {
            return
        }

        val roles = event.getMember.getRoles
        val userRole = if (!roles.isEmpty) Some(roles.get(0).getName()) else None

        val rawConfig = config.discordMessage
            .replace("$display_name", event.getAuthor.getEffectiveName)
            .replace("$handle", event.getAuthor.getName)
            .replace("$role", userRole.getOrElse(config.defaultRole))

        val discordMessageComponent: Component = minecraftSerializer.serialize(event.getMessage.getContentDisplay)
        val discordMessageLegacy = legacySerializer.serialize(discordMessageComponent)
        val finalLegacyMessage = rawConfig.replace("$message", discordMessageLegacy)
        val finalComponent = legacySerializer.deserialize(finalLegacyMessage)

        if (!isFolia()) {
            new BukkitRunnable {
                override def run(): Unit = Bukkit.broadcast(finalComponent)
            }.runTask(plugin)
        } else {
            Bukkit.getGlobalRegionScheduler.execute(plugin, new Runnable {
                override def run(): Unit = Bukkit.broadcast(finalComponent)
            })
        }
    }

    override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
        val players = Bukkit.getOnlinePlayers.asScala
        event.getName match {
            case "list" =>
                val playerNames = if (players.isEmpty) {
                    "No players are currently online."
                } else {
                    players.map(_.getName).mkString(", ")
                }
                event
                    .reply(s"Online Players: $playerNames")
                    .setEphemeral(true)
                    .queue()
            case "msg" =>
                val playerName = event.getOption("player").getAsString
                val message = event.getOption("message").getAsString
                val player = Bukkit.getPlayer(playerName)

                messageCommand(message, player, event)
            case _ => // Ignore unknown commands
        }
    }

    private def messageCommand(message: String, player: Player, event: SlashCommandInteractionEvent): Unit = {
        val playerName = player.getName

        if (player == null || !player.isOnline) {
            event
                .reply(s"Player '$playerName' is not online or does not exist.")
                .setEphemeral(true)
                .queue()
            return
        }

        val user = event.getUser
        replyTargets(player.getUniqueId) = (user.getId, user.getName)

        val nameComponent = Component.text(event.getUser.getName, NamedTextColor.DARK_AQUA)
        val messageComponent = Component.text(message, NamedTextColor.GRAY)
        val richMessage = miniMessage.deserialize(
            "<dark_gray>(<gray><sender> -> <dark_green>you</dark_green></gray>)</dark_gray> <message><br><gray><italic>You can reply with /reply <message></italic></gray>",
            Placeholder.component("sender", nameComponent),
            Placeholder.component("message", messageComponent)
        )

        if (!isFolia()) {
            new BukkitRunnable {
                override def run(): Unit = player.sendMessage(richMessage)
            }.runTask(plugin)
        } else {
            Bukkit.getGlobalRegionScheduler.execute(plugin, new Runnable {
                override def run(): Unit = player.sendMessage(richMessage)
            })
        }

        event
            .reply(s"Message sent to $playerName!")
            .setEphemeral(true)
            .queue()
    }

    private def isFolia(): Boolean = {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch {
            case _: ClassNotFoundException => false
        }
    }
}
