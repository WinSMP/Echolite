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
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

object ReplyCommand {

    fun createAndRegisterCommand(
        plugin: Echolite,
        discordBotManager: DiscordBotManager,
        replyTargets: ConcurrentHashMap<UUID, Pair<String, String>>
    ) {
        plugin.lifecycleManager.registerEventHandler(
            LifecycleEvents.COMMANDS,
            // The lambda argument here is the ReloadableRegistrarEvent
            { event: ReloadableRegistrarEvent<Commands> ->
                val replyCommand: LiteralArgumentBuilder<CommandSourceStack> = Commands
                    .literal("reply")
                    .requires { source -> source.sender is Player }
                    .then(
                        Commands
                            .argument("message", StringArgumentType.greedyString())
                            .executes { ctx ->
                                executeReplyCommand(ctx, plugin, discordBotManager, replyTargets)
                            }
                    )

                // register a short alias "r" too for convenience
                val rCommand: LiteralArgumentBuilder<CommandSourceStack> = Commands
                    .literal("r")
                    .requires { source -> source.sender is Player }
                    .then(
                        Commands
                            .argument("message", StringArgumentType.greedyString())
                            .executes { ctx ->
                                executeReplyCommand(ctx, plugin, discordBotManager, replyTargets)
                            }
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

    private fun executeReplyCommand(
        ctx: CommandContext<CommandSourceStack>,
        plugin: Echolite,
        discordBotManager: DiscordBotManager,
        replyTargets: ConcurrentHashMap<UUID, Pair<String, String>>
    ): Int {
        val sender = ctx.source.sender
        val player = sender as Player
        val playerUUID = player.uniqueId
        val message = StringArgumentType.getString(ctx, "message")

        val target = replyTargets[playerUUID] // Kotlin's map access returns null if not found
        if (target != null) {
            val (discordUserId, discordUserName) = target
            val privateMessage = PrivateMessage(playerUUID, message, discordUserId)
            discordBotManager.sendEmbedToUser(
                privateMessage,
                {
                    // DM succeeded - notify player on main thread
                    if (plugin.isFolia()) {
                        player.scheduler.run(
                            plugin,
                            { _ ->
                                player.sendMessage(
                                    Component.text(
                                        "Your message has been sent to ${discordUserName}. You can now reply to the bot to continue the conversation.",
                                        NamedTextColor.GREEN
                                    )
                                )
                                // Do not remove the reply target so the user can continue to reply
                            },
                            null
                        )
                    } else {
                        plugin.server.scheduler.runTask(
                            plugin,
                            Runnable {
                                player.sendMessage(
                                    Component.text(
                                        "Your message has been sent to ${discordUserName}. You can now reply to the bot to continue the conversation.",
                                        NamedTextColor.GREEN
                                    )
                                )
                                // Do not remove the reply target so the user can continue to reply
                            }
                        )
                    }
                },
                { _ ->
                    // DM failed (either false or an exception)
                    if (plugin.isFolia()) {
                        player.scheduler.run(
                            plugin,
                            { _ ->
                                player.sendMessage(
                                    Component.text(
                                        "Failed to send message to ${discordUserName} — the Discord user may have DMs disabled. You cannot reply to this user until they resolve this issue.",
                                        NamedTextColor.RED
                                    )
                                )
                            },
                            null
                        )
                    } else {
                        plugin.server.scheduler.runTask(
                            plugin,
                            Runnable {
                                player.sendMessage(
                                    Component.text(
                                        "Failed to send message to ${discordUserName} — the Discord user may have DMs disabled. You cannot reply to this user until they resolve this issue.",
                                        NamedTextColor.RED
                                    )
                                )
                            }
                        )
                    }
                    // keep the reply target so the player can retry after the user enables DMs
                }
            )

        } else {
            player.sendMessage(
                Component.text("You have no one to reply to.", NamedTextColor.RED)
            )
        }
        return Command.SINGLE_SUCCESS // Explicit return statement
    }

}

