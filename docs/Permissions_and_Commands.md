# Permissions and Commands

Global commands:
- `/menu [id]`: opens the main menu or the menu with id `[id]`.
- `/crmenu reload`: reloads configuration and menus.
- `/crmenu diag`: runs a configuration diagnostic and prints a report.
- `/crmenu regen`: regenerates `config.yml` from the embedded resource and reloads.

Permission nodes (plugin defaults):
- `crmenu.open`: allows opening menus (default: granted to everyone)
- `crmenu.reload`: allows reload/regeneration (default: `op`)
- `crmenu.diag`: allows diagnostics (default: `op`)

Configurable permissions in `config.yml`:
- `permissions.command_open`
- `permissions.admin_reload`
- `permissions.admin_diag`

Per-menu access control:
- `permission_open` in each menu file restricts opening that specific menu.

Per-item access control:
- `permission` in each item restricts clicking that item.