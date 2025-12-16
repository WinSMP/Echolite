// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

// Configuration case class from Echolite.scala
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