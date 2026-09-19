// SPDX-License-Identifier: GPL-3.0-only

package org.winlogon.echolite

/**
  * Returns a string escaped so Discord's Markdown formatter keeps identifiers such as usernames
  * the same.
  */
fun String.getDiscordCompatible(): String {
    return if (isEmpty() || !contains('_')) {
        this
    } else {
        // (?<!_)_(?!_) replaces single underscores not flanked by other underscores
        this.replace(Regex("(?<!_)_(?!_)")) { "\\_" }
    }
}
