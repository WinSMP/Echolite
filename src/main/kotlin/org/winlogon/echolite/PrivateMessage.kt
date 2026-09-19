// SPDX-License-Identifier: GPL-3.0-only
package org.winlogon.echolite

import java.util.UUID

data class PrivateMessage(
    val sender: UUID,
    val message: String,
    val recipient: String
)
