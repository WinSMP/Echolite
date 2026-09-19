// SPDX-License-Identifier: GPL-3.0-only
package org.winlogon.echolite

data class Configuration(
    val token: String,
    val channelId: String,
    val defaultRole: String,
    val sendStatusMessages: Boolean,
    val sendPlayerJoinMessages: Boolean,
    val sendPlayerDeathMessages: Boolean,
    val statusList: List<String>,
    val discordMessage: String,
    val minecraftMessage: String,
)
