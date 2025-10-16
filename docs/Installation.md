# Installation

Requirements:
- Java 8+ (Java 17 recomendado para servidores 1.20+)
- Servidor Spigot o Paper (compatibilidad 1.8 → 1.21.8, `api-version: 1.13`)
- Maven (si compilas desde el código)

Build from source:
1) Run `mvn package` in the project root.
2) The artifact is produced at `target/CrMenu-1.1.0.jar`.

Deploy:
1) Copia el JAR al directorio `plugins/` de tu servidor.
2) Inicia el servidor para generar configuración e idiomas por defecto.
3) En servidores 1.8–1.12, usa materiales legacy (p. ej. `RED_STAINED_GLASS_PANE` se convierte a `STAINED_GLASS_PANE` con color). En 1.13+ se usan materiales modernos.

Optional: install PlaceholderAPI for advanced placeholders.