// SPDX-License-Identifier: GPL-3.0-only
package org.winlogon.echolite

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.winlogon.asynccraftr.AsyncCraftrExecutors
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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
                    // Avoid causing comamnd registration conflicts with other plugins
                    .literal("discord-reply")
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

        val entityExecutor = AsyncCraftrExecutors.entity(plugin, player)

        val target = replyTargets[playerUUID] ?: run {
            player.sendRichMessage("<red>You have no one to reply to.</red>")
            return Command.SINGLE_SUCCESS;
        }

        val (discordUserId, discordUserName) = target
        val privateMessage = PrivateMessage(playerUUID, message, discordUserId)

        discordBotManager.sendEmbedToUser(
            privateMessage,
            // DM succeeded - notify player on main thread
            {
                entityExecutor.execute {
                    player.sendRichMessage(
                        """
                        <green>Your message has been sent to ${discordUserName}.
                        You can now reply to the bot to continue the conversation.</green>
                        """.trimIndent()
                    )
                    // Do not remove the reply target so the user can continue to reply
                }
            },

            // DM failed (either false or an exception)
            { _ ->
                entityExecutor.execute {
                    player.sendRichMessage(
                        """
                        <red>Failed to send message to ${discordUserName}. The Discord user may have DMs disabled.
                        You cannot reply to this user until they resolve this issue.</red>
                        """.trimIndent()
                    )
                }
                // Keep the reply target so the player can retry after the user enables DMs
            }
        )

        return Command.SINGLE_SUCCESS
    }
}
