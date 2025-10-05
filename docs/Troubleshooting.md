# Troubleshooting

Diagnostics:
- Run `/crmenu diag` to inspect your configuration. It will warn about:
  - Missing language directory or files
  - Empty titles or missing language keys
  - Invalid sizes (not multiples of 9 or out of range)
  - Missing `open_menu` targets

Common issues:
- "No permission": ensure players have `crmenu.open`, and check `permission_open` or per-item `permission`.
- Placeholders not resolving: install PlaceholderAPI and expansions, run `/papi reload`.
- Menu not opening: verify the menu id exists, permissions, and that the file is under `plugins/CrMenu/menus/`.
- Head textures: confirm `head_texture` is a valid Base64 string or use `head_texture_url`.

Regenerate configuration:
- Use `/crmenu regen` to overwrite `config.yml` with the embedded default and reload the plugin.