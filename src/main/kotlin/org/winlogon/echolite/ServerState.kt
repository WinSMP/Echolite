// SPDX-License-Identifier: MPL-20
package org.winlogon.echolite

import org.bukkit.plugin.PluginManager
import java.util.logging.Logger

data class ServerState(
    val logger: Logger,
    val config: Configuration,
    val pluginManager: PluginManager,
    val plugin: Echolite,
)
