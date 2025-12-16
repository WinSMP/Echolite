// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import org.bukkit.Bukkit

import java.util.UUID
import java.util.concurrent.*
import kotlin.random.Random
import java.util.logging.Logger

class DiscordBotManager(
    private val state: ServerState,
    private val replyTargets: ConcurrentHashMap<UUID, Pair<String, String>>
) {
    private var jda: JDA? = null
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val logger: Logger = state.logger

    // Using a cached thread pool for asynchronous operations, similar to Scala's ExecutionContext
    private val asyncExecutor: ExecutorService = Executors.newCachedThreadPool()

    fun startBot() {
        CompletableFuture.runAsync({
            logger.info("Starting Discord bot (awaiting ready)...")
            val bot = JDABuilder
                .createDefault(state.config.token)
                .addEventListeners(DiscordChatBridge(state.plugin, state.config, replyTargets))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build()
                .awaitReady()

            logger.info("Discord bot ready, registering commands & status cycling")

            jda = bot

            val commands = bot.updateCommands()
            commands.addCommands(
                Commands.slash("list", "Show the list of online players"),
                Commands.slash("msg", "Send a one-way message to a Minecraft player")
                    .addOption(OptionType.STRING, "player", "The name of the player to message", true)
                    .addOption(OptionType.STRING, "message", "The message to send", true)
            )
            commands.queue()

            if (state.config.sendStatusMessages) {
                sendMessageToDiscord("**Server Status** The server is online.")
            }
            startStatusCycling()

        }, asyncExecutor).exceptionally { e ->
            logger.severe("Failed to initialize Discord bot: ${e.message}")
            state.pluginManager.disablePlugin(state.plugin)
            null // Return null as there's no meaningful result
        }
    }

    fun shutdownBot() {
        if (state.config.sendStatusMessages) {
            sendMessageToDiscord("**Server Status** The server is shutting down.")
        }
        jda?.shutdown()
        scheduler.shutdown()
        asyncExecutor.shutdown() // Shutdown the async executor as well
        jda = null
    }

    fun sendMessageToDiscord(message: String) {
        jda?.let { bot ->
            val channel = bot.getTextChannelById(state.config.channelId)

            if (channel != null) {
                CompletableFuture.runAsync({
                    channel.sendMessage(message).queue(
                        { /* Message sent successfully */ },
                        { throwable ->
                            logger.severe("Failed to send message to Discord: ${throwable.message}")
                        }
                    )
                }, asyncExecutor).exceptionally { e ->
                    logger.severe("Failed to send message to Discord (async error): ${e.message}")
                    null
                }
            } else {
                logger.severe("Discord channel not found. Please check the channel ID in the config.")
            }
        }
    }

    private fun startStatusCycling() {
        fun scheduleNextStatusChange() {
            // Random delay between 5 and 10 minutes
            val randomDelay = (5 + Random.nextInt(6)).toLong()
            scheduler.schedule(
                {
                    val newStatus = state.config.statusList[Random.nextInt(state.config.statusList.size)]
                    jda?.presence?.setActivity(Activity.playing(newStatus))
                    scheduleNextStatusChange()
                },
                randomDelay,
                TimeUnit.MINUTES
            )
        }
        scheduleNextStatusChange()
    }

    fun isFolia(): Boolean {
        return try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    fun getJDA(): JDA? = jda

    fun sendPrivateMessageToDiscord(userId: String, message: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        jda?.let { bot ->
            bot.retrieveUserById(userId).queue { user ->
                user.openPrivateChannel().queue { privateChannel ->
                    privateChannel.sendMessage(message).queue(
                        { onSuccess() },
                        { throwable -> onFailure(throwable) }
                    )
                }
            }
        }
    }

    fun sendEmbedToUser(privateMessage: PrivateMessage, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        jda?.let { bot ->
            bot.retrieveUserById(privateMessage.recipient).queue { user ->
                user.openPrivateChannel().queue { privateChannel ->
                    val player = Bukkit.getOfflinePlayer(privateMessage.sender)
                    val embed = EmbedBuilder()
                        .setTitle("New Message")
                        .setDescription(
                            "Hello! Player ${player.name} (`${privateMessage.sender}`) has sent you a message: **${privateMessage.message}**"
                        )
                        .build()
                    privateChannel.sendMessageEmbeds(embed).queue(
                        { onSuccess() },
                        { throwable -> onFailure(throwable) }
                    )
                }
            }
        }
    }
}