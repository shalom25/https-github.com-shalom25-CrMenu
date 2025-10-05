# Languages and PlaceholderAPI

Languages:
- Language files live under `plugins/CrMenu/lang/`.
- Defaults: `en.yml` and `es.yml` are provided.
- Set `language: en` or `language: es` in `config.yml`.
- Refer to language keys in any text using `@lang:<key>` (e.g., `@lang:menu_title`).
- Common keys include: `prefix`, `menu_title`, `open_message`, `no_permission`.

Colors:
- Use `&` color/style codes in titles, names, and lore.

PlaceholderAPI integration:
- If PlaceholderAPI is present, `%placeholders%` in text are resolved using PAPI.
- Supported in inventory titles, item names, lore, and action messages/commands.
- Install PAPI and expansions: `/papi ecloud download <expansion>`, then `/papi reload`.

Internal tokens:
- `{prefix}` is replaced by the language `prefix`.
- `%player_ping%` is resolved even without PAPI.