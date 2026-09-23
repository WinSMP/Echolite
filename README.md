# Echolite

Echolite is a lightweight Kotlin‑based Minecraft plugin that bridges your Paper (or Folia) server's chat with a Discord channel.

Messages sent on your Minecraft server will relayed to Discord, and Discord messages (from a specified channel) will be broadcast back into Minecraft.

## Features

- **Slash commands** (`/list`, `/msg`) to interact with online players directly from Discord
- **Status cycling** for your bot presence (configurable list, randomized every 5–10 minutes)
- **Join/leave/death announcements** in Discord (toggleable)
- **Fully configurable** message formats with placeholders
- **Private messaging** (`/msg`, `/discord-reply`) with persistent reply targets
- **Folia support**

## Requirements

- **Server**:
    - Java 25+
    - A [Paper](https://papermc.io) or any of its forks, like [Purpur](https://purpurmc.org/).
- **Bot**:
    - A Discord bot token with `MESSAGE_CONTENT` intent enabled and permission to read/send messages in the target channel

## Installation

1. **Build**
   ```bash
   ./gradlew build
   ```
2. **Deploy**
   - Move `Echolite.jar` into your server's `plugins/` directory.
   - Restart your server to generate the default `config.yml`.

## Configuration

After first run, edit `plugins/Echolite/config.yml` with:

```yaml
discord:
  token: "YOUR_BOT_TOKEN"
  channel-id: "YOUR_CHANNEL_ID"
  default-role: "Member"              # used if Discord user has no roles
  status-messages: true               # broadcast online/offline status
  player-join-messages: true          # announce joins/quits in Discord
  player-death-messages: true         # announce death messages
  status-list:                        # activities your bot will cycle through
    - "Fighting creepers"
    - "Mining diamonds"
    - "Exploring caves"

message:
  discord: "$display_name ($role): $message"
  minecraft: "<$user_name> $message"
```

### Configuration parameters

|Key|Type|Description|
|---|---|---|
|`discord.token`|string|Your Discord bot’s token.|
|`discord.channel-id`|string|The ID of the Discord channel for relaying Minecraft chat.|
|`discord.default-role`|string|Shown in `$role` when a user has no Discord roles.|
|`discord.status-messages`|boolean|Whether to announce “Server online” / “Server shutting down” in Discord.|
|`discord.player-join-messages`|boolean| Whether to announce player joins/quits in Discord.|
|`discord.player-death-messages`|boolean| Whether to announce player deaths in Discord.|
|`discord.status-list`|list|A list of status messages your bot will cycle through every 5–10 minutes.|
|`message.discord`|string|Format for messages sent **to** Discord.|
|`message.minecraft`|string|Format for messages sent **to** Minecraft.|

## Placeholders

**Discord → Minecraft** (`message.minecraft`): 

|Placeholder|Processed value|
|---|---|
|`$user_name`|Bukkit player name|
|`$message`|Raw chat text|

**Minecraft → Discord** (`message.discord`):

|Placeholder|Processed value|
|---|---|
|`$display_name`|Member’s Discord display name|
|`$handle`|Discord username (without discriminator)|
|`$role`|First role name or `default-role`|
|`$message`|Message content, serialized via Adventure→legacy|

## Slash Commands

Echolite registers two slash commands (with ephemeral replies) when the bot starts:

- `/list`: Lists all online Minecraft
- `/msg <player> <message>`: Sends a one‑way message to a player

## Roadmap

- [X] Two‑way private messaging support (Discord ↔ Minecraft)

## License

Licensed under the [GPL-3.0](LICENSE) license.
