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
import org.bukkit.Bukkit
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent

import com.mojang.brigadier.Command

import java.util.UUID
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object ReplyCommand {

    def createAndRegisterCommand(
        plugin: Echolite,
        discordBotManager: DiscordBotManager,
        replyTargets: mutable.Map[UUID, (String, String)]
    ): Unit = {
        plugin.getLifecycleManager.registerEventHandler(
            LifecycleEvents.COMMANDS,
            (event: ReloadableRegistrarEvent[io.papermc.paper.command.brigadier.Commands]) => {
                val replyCommand: LiteralArgumentBuilder[CommandSourceStack] = Commands
                    .literal("reply")
                    .requires(source => source.getSender.isInstanceOf[Player])
                    .`then`(
                        Commands
                            .argument("message", StringArgumentType.greedyString())
                            .executes(ctx =>
                                executeReplyCommand(ctx, plugin, discordBotManager, replyTargets)
                            )
                    )

                // register a short alias "r" too for convenience
                val rCommand: LiteralArgumentBuilder[CommandSourceStack] = Commands
                    .literal("r")
                    .requires(source => source.getSender.isInstanceOf[Player])
                    .`then`(
                        Commands
                            .argument("message", StringArgumentType.greedyString())
                            .executes(ctx =>
                                executeReplyCommand(ctx, plugin, discordBotManager, replyTargets)
                            )
                    )

                event
                    .registrar()
                    .register(
                        replyCommand.build(),
                        "Reply to the last Discord message received from a user."
                    )
                event.registrar().register(rCommand.build(), "Short alias for reply.")
            }
        )
    }

    private def executeReplyCommand(
        ctx: CommandContext[CommandSourceStack],
        plugin: Echolite,
        discordBotManager: DiscordBotManager,
        replyTargets: mutable.Map[UUID, (String, String)]
    ): Int = {
        val sender = ctx.getSource.getSender
        val player = sender.asInstanceOf[Player]
        val playerUUID = player.getUniqueId
        val message = StringArgumentType.getString(ctx, "message")

        replyTargets.get(playerUUID) match {
            case Some((discordUserId, discordUserName)) =>
                val privateMessage = PrivateMessage(playerUUID, message, discordUserId)
                discordBotManager.sendEmbedToUser(
                    privateMessage,
                    () => {
                        // DM succeeded - notify player on main thread
                        if (plugin.isFolia) {
                            player.getScheduler.run(
                                plugin,
                                task => {
                                    player.sendMessage(
                                        Component.text(
                                            s"Your message has been sent to ${discordUserName}. You can now reply to the bot to continue the conversation.",
                                            NamedTextColor.GREEN
                                        )
                                    )
                                    // Do not remove the reply target so the user can continue to reply
                                },
                                null
                            )
                        } else {
                            plugin.getServer.getScheduler.runTask(
                                plugin,
                                new Runnable {
                                    override def run(): Unit = {
                                        player.sendMessage(
                                            Component.text(
                                                s"Your message has been sent to ${discordUserName}. You can now reply to the bot to continue the conversation.",
                                                NamedTextColor.GREEN
                                            )
                                        )
                                        // Do not remove the reply target so the user can continue to reply
                                    }

                                }
                            )
                        }
                    },
                    _ => {
                        // DM failed (either false or an exception)
                        if (plugin.isFolia) {
                            player.getScheduler.run(
                                plugin,
                                task => {
                                    player.sendMessage(
                                        Component.text(
                                            s"Failed to send message to ${discordUserName} — the Discord user may have DMs disabled. You cannot reply to this user until they resolve this issue.",
                                            NamedTextColor.RED
                                        )
                                    )
                                },
                                null
                            )
                        } else {
                            plugin.getServer.getScheduler.runTask(
                                plugin,
                                new Runnable {
                                    override def run(): Unit = {
                                        player.sendMessage(
                                            Component.text(
                                                s"Failed to send message to ${discordUserName} — the Discord user may have DMs disabled. You cannot reply to this user until they resolve this issue.",
                                                NamedTextColor.RED
                                            )
                                        )
                                    }
                                }
                            )
                        }
                        // keep the reply target so the player can retry after the user enables DMs
                    }
                )

            case None =>
                player.sendMessage(
                    Component.text("You have no one to reply to.", NamedTextColor.RED)
                )
        }
        Command.SINGLE_SUCCESS
    }

}
