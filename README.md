# CrMenu

Plugin Spigot/Bukkit para crear un menú del servidor totalmente configurable y con soporte de múltiples idiomas.

## Wiki (English)

- Full documentation in English is available under the `docs/` folder:
  - `docs/Overview.md` (plugin description and features)
  - `docs/Installation.md`
  - `docs/Configuration.md`
  - `docs/Menus.md`
  - `docs/Actions.md`
  - `docs/Permissions_and_Commands.md`
  - `docs/Languages_and_PlaceholderAPI.md`
  - `docs/Troubleshooting.md`

## Requisitos

- Java 17 (Minecraft 1.20+ requiere Java 17)
- Maven instalado (o importe el proyecto en IntelliJ/VSCode y use el soporte de Maven)
- Servidor Spigot/Paper compatible (api-version 1.20)

## Instalación

1. Compila el proyecto:
   - En consola: `mvn package`
- El artefacto se generará en `target/CrMenu-1.1.0.jar`
2. Copia el `.jar` a la carpeta `plugins` de tu servidor.
3. Inicia el servidor para que se generen los archivos de configuración y lenguaje.

## Configuración

### Estructura de menús en carpeta `menus/`

Puedes organizar submenús en una carpeta separada ubicada en `plugins/CrMenu/menus/`.
Coloca múltiples archivos `.yml` dentro de esa carpeta; cada archivo define un menú con su propio `title`, `size`, `permission_open`, `border`, `back_button` e `items`.

- `menus/main.yml`: menú principal recomendado.
- `menus/info.yml`, `menus/shop.yml`, `menus/admin.yml`: ejemplos opcionales de diferentes tipos de menú.

El plugin carga todos los archivos `.yml` de la carpeta `menus/`. Si no existen, se creará la carpeta y, si el JAR incluye recursos por defecto, se guardarán automáticamente al iniciar.

Si no hay archivos en `menus/`, el plugin hace fallback a `config.yml` con la estructura anterior.

Ejemplo rápido (`menus/main.yml`):

```yml
title: "&6&lCrMenu &7- &fPrincipal"
size: 27
permission_open: "crmenu.open.main" # opcional
border:
  enabled: true
  material: "GRAY_STAINED_GLASS_PANE"
  name: " "
back_button:
  enabled: true
  slot: 26
  material: "ARROW"
  name: "@lang:back_button"
  target_menu: "main"
items:
  - slot: 11
    material: "PAPER"
    name: "&aInformación"
    lore:
      - "&7Consulta detalles de tu cuenta"
    action: "open_menu"
    value: "info"
  - slot: 13
    material: "EMERALD"
    name: "&bTienda"
    lore:
      - "&7Compra ítems útiles"
    action: "open_menu"
    value: "shop"
```

Permisos:

- Globales configurables en `config.yml` sección `permissions`: `command_open`, `admin_reload`, `admin_diag`.
- Por menú (`permission_open` en el archivo del menú) para restringir apertura.
- Por ítem (`permission` en cada item) para restringir clics.

Archivo `config.yml`:

- `language`: Código de idioma por defecto (`es` o `en`).
- `menu.title`: Título del menú. Puede ser un texto o una referencia de idioma usando `@lang:clave`.
- `menu.size`: Tamaño del inventario (múltiplos de 9).
- `menu.items`: Lista de ítems del menú con:
  - `slot`: posición del ítem.
  - `material`: material del ítem (nombre de `Material` de Spigot).
  - `name`: nombre visible (admite color `&` y placeholders).
  - `lore`: líneas de descripción (admiten color `&` y placeholders).
  - `action.type`: `command` (jugador), `command_console`, `message`.
  - `action.value`: contenido del comando o mensaje.

Placeholders disponibles:

- `{prefix}`: prefijo definido en el archivo de idioma.

## Soporte de PlaceholderAPI

CrMenu resuelve placeholders de PlaceholderAPI en:
- Título del inventario
- Nombres y lore de ítems del menú
- Acciones `MESSAGE`, `COMMAND_PLAYER` y `COMMAND_CONSOLE`

Requisitos:
- Instala PlaceholderAPI en el servidor.
- Descarga las expansiones necesarias con `/papi ecloud download <expansion>` y ejecuta `/papi reload`.

Uso:
- Escribe placeholders en `config.yml` como `%player_name%`, `%server_tps%`, `%player_ping%`, etc.
- Los placeholders se combinan con `{prefix}` del plugin cuando corresponda.

Ejemplo:
```yml
menus:
  info:
    title: "&bInfo de %player_name%"
    size: 27
    items:
      - slot: 13
        material: PAPER
        name: "&aTu ping: %player_ping%ms"
        lore:
          - "Conectado: %server_online% jugadores"
        action:
          type: message
          value: "Hola %player_name%, tu ping es %player_ping%ms"
```

## Idiomas

Los archivos de idioma están en `src/main/resources/lang`. Por defecto se copian a la carpeta de datos del plugin la primera vez.

Claves disponibles:

- `prefix`
- `menu_title`
- `open_message`
- `no_permission`

Puedes añadir más claves o nuevos archivos `lang/<codigo>.yml` y cambiar `language` en la `config.yml`.

## Uso

- Comando: `/menu` (alias `/crmenu`)
- Permiso: `crmenu.open` (por defecto concedido a todos)

Al ejecutar el comando se abrirá el menú definido en `config.yml`. Los clics se manejan según el `action` configurado en cada ítem.

## Notas

- Si cambias la configuración, reinicia o usa `/reload` con precaución. Para reconstruir el menú sin reiniciar, puedes recargar el plugin o reabrir el menú.
- Este proyecto usa Spigot API como dependencia `provided`; no se empaqueta en el JAR final.

## Compatibilidad

- Versiones de Minecraft compatibles: 1.8 → 1.21.8.
- Java: el JAR se compila para Java 8 (funciona en Java 8–21). Servidores 1.20+ requieren ejecutar Java 17+.
- `api-version`: 1.13 para materiales modernos; en 1.8–1.12 se hace fallback automático a materiales legacy.
- Sonidos: se intenta `UI_TOAST_IN` y se hace fallback a `CLICK`/`LEVEL_UP` en servidores antiguos.