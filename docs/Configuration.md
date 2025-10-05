# Configuration

Global configuration is in `plugins/CrMenu/config.yml`. It may also contain menu definitions when the `menus/` folder is not used.

Important keys:
- `language`: default language code (`en` or `es`).
- `permissions`: configurable nodes
  - `command_open`: permission to use `/menu`
  - `admin_reload`: permission for reload/regeneration
  - `admin_diag`: permission to run diagnostics
- `border`: global border settings: `enabled`, `material`, `name`
- `back_button`: global back button settings: `enabled`, `material`, `name`, `slot`

Colors and text:
- Use `&` color codes (e.g., `&a`, `&b`, `&c`, `&l`) in `title`, `name`, and `lore`.
- Reference language keys via `@lang:<key>`.
- The `{prefix}` token is replaced by the language `prefix`.

Heads and materials:
- Accepts `Material` names and namespaced values like `minecraft:compass`.
- Player/custom heads:
  - `head_owner`: player name
  - `head_texture`: Base64 texture value
  - `head_texture_url`: URL to textures.minecraft.net (auto-encoded to Base64)

Fallback behavior:
- If there are no files under `plugins/CrMenu/menus/`, CrMenu falls back to `config.yml` menu structure.