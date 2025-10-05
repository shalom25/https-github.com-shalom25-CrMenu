# Overview

CrMenu is a Spigot/Paper plugin that provides fully configurable server menus driven by YAML files, with multi-language support and optional PlaceholderAPI integration. It is ideal for navigation hubs, info panels, shop access, and admin dashboards.

Key capabilities:
- Unlimited menus using separate YAML files under `plugins/CrMenu/menus/`.
- Flexible visual layout: configurable `title`, `size` (multiples of 9), `border`, and `back_button` per menu.
- Fine-grained access control: global permission for opening, per-menu `permission_open`, and per-item `permission`.
- Item actions: run player or console commands, send messages, or open another menu.
- Heads and materials: supports standard `Material`, namespaced `minecraft:*`, and player/custom heads via `head_owner`, `head_texture`, or `head_texture_url`.
- Multi-language: `lang/en.yml`, `lang/es.yml` with `@lang:<key>` references.
- PlaceholderAPI: resolve placeholders in titles, names, lore, and action values.
- Admin utilities: reload configuration, run diagnostics, and regenerate the default config.

Support matrix:
- Minecraft API: `api-version: 1.20` (Spigot/Paper)
- Java 17 required
- PlaceholderAPI optional