// SPDX-License-Identifier: MPL-2.0
package org.winlogon.echolite

import java.util.UUID

case class PrivateMessage(
    sender: UUID,
    message: String,
    recipient: String,
)
