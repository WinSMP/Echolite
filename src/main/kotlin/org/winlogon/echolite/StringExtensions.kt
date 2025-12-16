package org.winlogon.echolite

// This extension function is moved from MinecraftChatBridge.kt to make it testable and reusable.
fun String.getDiscordCompatible(): String {
    return if (isEmpty() || !contains('_')) {
        this
    } else {
        // (?<!_)_(?!_) replaces single underscores not flanked by other underscores
        this.replace(Regex("(?<!_)_(?!_)")) { "\\_" }
    }
}

