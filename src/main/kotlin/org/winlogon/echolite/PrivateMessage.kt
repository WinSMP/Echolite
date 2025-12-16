// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

import java.util.UUID

data class PrivateMessage(
    val sender: UUID,
    val message: String,
    val recipient: String
)
