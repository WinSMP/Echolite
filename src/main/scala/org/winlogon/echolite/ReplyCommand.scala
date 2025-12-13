// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent

import com.mojang.brigadier.Command

import java.util.UUID
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object ReplyCommand {

  def createAndRegisterCommand(plugin: Echolite, discordBotManager: DiscordBotManager, replyTargets: mutable.Map[UUID, (String, String)]): Unit = {
    plugin.getLifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, (event: ReloadableRegistrarEvent[io.papermc.paper.command.brigadier.Commands]) => {
      val replyCommand: LiteralArgumentBuilder[CommandSourceStack] = Commands.literal("reply")
        .requires(source => source.getSender.isInstanceOf[Player])
        .`then`(
          Commands.argument("message", StringArgumentType.greedyString())
            .executes(ctx => executeReplyCommand(ctx, discordBotManager, replyTargets))
        )
      event.registrar().register(replyCommand.build(), "Reply to the last Discord message received from a user.")
    })
  }

  private def executeReplyCommand(ctx: CommandContext[CommandSourceStack], discordBotManager: DiscordBotManager, replyTargets: mutable.Map[UUID, (String, String)]): Int = {
    val sender = ctx.getSource.getSender
    val player = sender.asInstanceOf[Player]
    val playerUUID = player.getUniqueId
    val message = StringArgumentType.getString(ctx, "message")

    replyTargets.get(playerUUID) match {
      case Some((discordUserId, discordUserName)) =>
        discordBotManager.sendPrivateMessageToDiscord(
          discordUserId,
          s"**${player.getName}** (UUID: `${playerUUID}`) has replied to you: ${message}"
        )
        player.sendMessage(
          Component.text(s"Your message has been sent to ${discordUserName}.", NamedTextColor.GREEN)
        )
        replyTargets.remove(playerUUID)
      case None =>
        player.sendMessage(
          Component.text("You have no one to reply to.", NamedTextColor.RED)
        )
    }
    Command.SINGLE_SUCCESS
  }
}