# Actions

Actions are triggered when players click on items in a menu. Each action can optionally require a specific permission via the `permission` field on the item.

Supported action types:
- `command` / `command_player`: runs a command as the player
- `command_console`: runs a command from the server console
- `message`: sends a chat message to the player
- `open_menu`: opens another menu by id
- `back_menu`: returns to the parent menu (or the default menu if no parent)

Placeholders in actions:
- All text is processed with language colors and the `{prefix}` token.
- If PlaceholderAPI is installed, `%placeholders%` are resolved via PAPI.
- Some internal tokens like `%player_ping%` are supported without PAPI.

Examples:
```yml
# Console command with player name via PAPI
action:
  type: command_console
  value: "spawn %player_name%"

# Player command
action:
  type: command
  value: "warp arena"

# Message
action:
  type: message
  value: "&aHello %player_name%, welcome!"

# Open another menu
action:
  type: open_menu
  value: "info"
```