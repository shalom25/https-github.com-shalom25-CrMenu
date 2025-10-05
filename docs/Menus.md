# Menus (Multi-file Layout)

CrMenu can load multiple menus from YAML files located in `plugins/CrMenu/menus/`. Each file is a single menu with its own visual and behavioral configuration.

Common fields per menu file:
- `title`: inventory title. Supports `&` colors and `@lang:<key>`.
- `size`: inventory size. Must be a multiple of 9 (9–54).
- `permission_open`: optional permission required to open this specific menu.
- `border`: optional per-menu override: `enabled`, `material`, `name`.
- `back_button`: optional per-menu override: `enabled`, `material`, `name`, `slot`, `target_menu`.
- `items`: list of clickable items with fields:
  - `slot`: integer slot position
  - `material`: item material (e.g., `STONE`, `minecraft:compass`)
  - `name`: display name; supports colors and `@lang`
  - `lore`: array of description lines; supports colors and placeholders
  - `permission`: optional required permission to click this item
  - `action`: object with `type` and `value`

Action types:
- `command` (or `command_player`): execute as the player
- `command_console`: execute from server console
- `message`: send a chat message to the player
- `open_menu`: open another menu by id
- `back_menu`: go back to the parent or default menu

Back button:
- A `back_button` section can automatically add an item that opens `target_menu`.
- Alternatively, you can add a custom item with `action.type: back_menu`.

Example (`menus/main.yml`):
```yml
title: "&6&lCrMenu &7- &fMain"
size: 27
permission_open: "crmenu.open.main" # optional
border:
  enabled: true
  material: GRAY_STAINED_GLASS_PANE
  name: " "
back_button:
  enabled: true
  slot: 26
  material: ARROW
  name: "@lang:back_button"
  target_menu: "main"
items:
  - slot: 11
    material: PAPER
    name: "&aInformation"
    lore:
      - "&7View your account details"
    action:
      type: open_menu
      value: info
  - slot: 13
    material: EMERALD
    name: "&bShop"
    lore:
      - "&7Buy useful items"
    action:
      type: open_menu
      value: shop
```