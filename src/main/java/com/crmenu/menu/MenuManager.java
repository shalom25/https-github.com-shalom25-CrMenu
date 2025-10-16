package com.crmenu.menu;

import com.crmenu.util.Language;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuManager {

    private final Plugin plugin;
    private Language language;

    // Menús múltiples: id -> inventario y acciones
    private final Map<String, Inventory> inventories = new HashMap<>();
    private final Map<String, Map<Integer, MenuAction>> menuActions = new HashMap<>();
    private final Map<String, String> menuTitles = new HashMap<>(); // id -> título sin color
    private final Map<String, String> titleToId = new HashMap<>(); // título coloreado -> id
    private final Map<String, String> menuParents = new HashMap<>(); // id -> id del menú padre
    private final Map<String, String> menuOpenPermissions = new HashMap<>(); // id -> permiso para abrir

    public MenuManager(Plugin plugin, Language language) {
        this.plugin = plugin;
        this.language = language;
        buildMenus();
    }

    public void openMenu(Player player) {
        // Abrir menú principal (compatibilidad con config legacy)
        String defaultId = getDefaultMenuId();
        openMenu(player, defaultId);
    }

    public void openMenu(Player player, String menuId) {
        if (menuId == null || menuId.trim().isEmpty()) menuId = getDefaultMenuId();
        // Validar permiso de apertura por menú si está configurado
        String openPerm = menuOpenPermissions.getOrDefault(menuId, "");
        if (openPerm != null && !openPerm.trim().isEmpty() && !player.hasPermission(openPerm)) {
            player.sendMessage(language.color(language.get("no_permission")));
            return;
        }
        Inventory base = inventories.get(menuId);
        if (base == null) {
            // Intentar reconstruir y reintentar
            buildMenus();
            base = inventories.get(menuId);
        }
        if (base != null) {
            // Crear una copia del inventario por jugador para resolver placeholders dinámicos
            String title = menuTitles.getOrDefault(menuId, "CrMenu");
            String dynamicTitle = language.color(applyPlaceholders(player, title));
            // Registrar el título dinámico para resolver el id en el listener
            titleToId.put(dynamicTitle, menuId);
            Inventory inv = Bukkit.createInventory(null, base.getSize(), dynamicTitle);
            for (int i = 0; i < base.getSize(); i++) {
                ItemStack src = base.getItem(i);
                if (src != null) {
                    ItemStack copy = src.clone();
                    ItemMeta meta = copy.getItemMeta();
                    if (meta != null) {
                        if (meta.hasDisplayName()) {
                            meta.setDisplayName(language.color(applyPlaceholders(player, meta.getDisplayName())));
                        }
                        if (meta.hasLore()) {
                            List<String> newLore = new ArrayList<>();
                            for (String line : meta.getLore()) {
                                newLore.add(language.color(applyPlaceholders(player, line)));
                            }
                            meta.setLore(newLore);
                        }
                        // Si es una cabeza de jugador sin propietario, asignar la del jugador actual
                        if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) meta;
                            try {
                                org.bukkit.OfflinePlayer owning = skullMeta.getOwningPlayer();
                                if (owning == null) {
                                    skullMeta.setOwningPlayer(player);
                                }
                            } catch (Throwable ignored) {
                                // Compatibilidad de versiones: si falla, omitir
                            }
                        }
                        copy.setItemMeta(meta);
                    }
                    inv.setItem(i, copy);
                }
            }
            player.openInventory(inv);
        } else {
            player.sendMessage(language.color("&cMenú no encontrado: " + menuId));
        }
    }

    public Listener getClickListener() {
        return new Listener() {
            @EventHandler
            public void onClick(InventoryClickEvent event) {
                if (!(event.getWhoClicked() instanceof Player)) return;
                if (event.getView() == null) return;
                String viewTitle = event.getView().getTitle();
                if (viewTitle == null) return;
                String id = titleToId.get(viewTitle);
                if (id == null) return;
                event.setCancelled(true);

                int slot = event.getRawSlot();
                Map<Integer, MenuAction> actions = menuActions.get(id);
                if (actions == null) return;
                MenuAction action = actions.get(slot);
                if (action == null) return;

                Player player = (Player) event.getWhoClicked();
                switch (action.type) {
                    case COMMAND_PLAYER:
                        if (action.permission != null && !action.permission.trim().isEmpty() && !player.hasPermission(action.permission)) {
                            player.sendMessage(language.color(language.get("no_permission")));
                            return;
                        }
                        player.closeInventory();
                        String cmdPlayer = applyPlaceholders(player, action.value);
                        // Evitar códigos de color en comandos
                        cmdPlayer = org.bukkit.ChatColor.stripColor(language.color(cmdPlayer));
                        player.performCommand(cmdPlayer);
                        break;
                    case COMMAND_CONSOLE:
                        if (action.permission != null && !action.permission.trim().isEmpty() && !player.hasPermission(action.permission)) {
                            player.sendMessage(language.color(language.get("no_permission")));
                            return;
                        }
                        player.closeInventory();
                        String cmdConsole = applyPlaceholders(player, action.value);
                        cmdConsole = org.bukkit.ChatColor.stripColor(language.color(cmdConsole));
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdConsole);
                        break;
                    case MESSAGE:
                        if (action.permission != null && !action.permission.trim().isEmpty() && !player.hasPermission(action.permission)) {
                            player.sendMessage(language.color(language.get("no_permission")));
                            return;
                        }
                        player.sendMessage(language.color(applyPlaceholders(player, action.value)));
                        break;
                    case OPEN_MENU:
                        if (action.permission != null && !action.permission.trim().isEmpty() && !player.hasPermission(action.permission)) {
                            player.sendMessage(language.color(language.get("no_permission")));
                            return;
                        }
                        player.closeInventory();
                        // Registrar el menú actual como padre del menú que se va a abrir
                        menuParents.put(action.value, id);
                        openMenu(player, action.value);
                        break;
                    case BACK_MENU:
                        player.closeInventory();
                        String parentId = menuParents.get(id);
                        if (parentId != null) {
                            openMenu(player, parentId);
                        } else {
                            // Si no hay padre, ir al menú principal
                            openMenu(player, getDefaultMenuId());
                        }
                        break;
                }
            }
        };
    }

    public List<String> diagnoseConfig() {
        List<String> report = new ArrayList<>();
        // Idioma
        String langCode = plugin.getConfig().getString("language", "en");
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            report.add("[WARN] Carpeta de idioma no existe: se creará automáticamente.");
        }
        File langFile = new File(langDir, langCode + ".yml");
        if (!langFile.exists()) {
            report.add("[WARN] Archivo de idioma faltante '" + langCode + ".yml'. Se generará o se usará 'en.yml'.");
        } else {
            report.add("[OK] Idioma '" + langCode + "' presente.");
        }

        // Modo multi-menús si existe 'menus', si no validar config legacy 'menu'
        ConfigurationSection menusSection = plugin.getConfig().getConfigurationSection("menus");
        if (menusSection != null) {
            if (menusSection.getKeys(false).isEmpty()) {
                report.add("[WARN] No hay menús definidos en 'menus'.");
            } else {
                for (String id : menusSection.getKeys(false)) {
                    ConfigurationSection def = menusSection.getConfigurationSection(id);
                    if (def == null) {
                        report.add("[ERROR] Menú '" + id + "' sección inválida.");
                        continue;
                    }
                    String rawTitle = def.getString("title", "Menu");
                    if (rawTitle.trim().isEmpty()) {
                        report.add("[WARN] Menú '" + id + "': title vacío.");
                    } else if (rawTitle.startsWith("@lang:")) {
                        String key = rawTitle.substring("@lang:".length());
                        String val = language.get(key);
                        if (key.equals(val)) {
                            report.add("[WARN] Menú '" + id + "': clave de idioma no encontrada '" + key + "'.");
                        }
                    }
                    int size = def.getInt("size", 27);
                    if (size % 9 != 0 || size < 9 || size > 54) {
                        report.add("[ERROR] Menú '" + id + "': size inválido " + size + ".");
                    }
                    List<?> itemsRaw = def.getList("items");
                    if (itemsRaw == null) itemsRaw = new ArrayList<>();
                    if (itemsRaw.isEmpty()) {
                        report.add("[WARN] Menú '" + id + "': sin ítems.");
                    } else {
                        report.add("[OK] Menú '" + id + "': ítems " + itemsRaw.size());
                        java.util.Set<Integer> seen = new java.util.HashSet<>();
                        int invSize = Math.max(9, Math.min(54, size));
                        int idx = 0;
                        for (Object obj : itemsRaw) {
                            idx++;
                            Map<String, Object> map = toStringObjectMap(obj);
                            if (map.isEmpty()) {
                                report.add("[ERROR] Menú '" + id + "' item #" + idx + ": no es mapa.");
                                continue;
                            }
                            int slot = safeInt(map.get("slot"), -1);
                            if (slot < 0 || slot >= invSize) {
                                report.add("[ERROR] Menú '" + id + "' item #" + idx + ": slot fuera de rango (" + slot + ").");
                            } else if (!seen.add(slot)) {
                                report.add("[WARN] Menú '" + id + "' item #" + idx + ": slot duplicado (" + slot + ").");
                            }
                            String materialName = String.valueOf(map.getOrDefault("material", "STONE"));
                            Material mat = normalizeMaterial(materialName);
                            if (mat == null) report.add("[WARN] Menú '" + id + "' item #" + idx + ": material inválido '" + materialName + "'.");
                            Map<String, Object> actionMap = toStringObjectMap(map.get("action"));
                            String typeStr = String.valueOf(actionMap.getOrDefault("type", "command"));
                            String value = String.valueOf(actionMap.getOrDefault("value", ""));
                            if (typeStr.equalsIgnoreCase("open_menu") && (value == null || value.trim().isEmpty())) {
                                report.add("[WARN] Menú '" + id + "' item #" + idx + ": open_menu sin id destino.");
                            }
                        }
                    }
                }
            }
        } else {
            // Legacy
            String rawTitle = plugin.getConfig().getString("menu.title", "Menu");
            if (rawTitle == null || rawTitle.trim().isEmpty()) {
                report.add("[WARN] menu.title vacío; se usará valor por defecto.");
            } else if (rawTitle.startsWith("@lang:")) {
                String key = rawTitle.substring("@lang:".length());
                String val = language.get(key);
                if (key.equals(val)) {
                    report.add("[WARN] Clave de idioma '" + key + "' no encontrada para el título.");
                } else {
                    report.add("[OK] Título resuelto por idioma ('" + key + "').");
                }
            } else {
                report.add("[OK] Título directo configurado.");
            }

            int size = plugin.getConfig().getInt("menu.size", 27);
            if (size % 9 != 0 || size < 9 || size > 54) {
                report.add("[ERROR] menu.size inválido: " + size + ". Debe ser múltiplo de 9 entre 9 y 54.");
            } else {
                report.add("[OK] Tamaño de inventario: " + size);
            }

            List<?> itemsRaw = plugin.getConfig().getList("menu.items");
            if (itemsRaw == null || itemsRaw.isEmpty()) {
                report.add("[WARN] No hay ítems configurados en menu.items.");
            } else {
                report.add("[OK] Ítems configurados: " + itemsRaw.size());
                int invSize = Math.max(9, Math.min(54, size));
                java.util.Set<Integer> seenSlots = new java.util.HashSet<>();
                int index = 0;
                for (Object obj : itemsRaw) {
                    index++;
                    if (!(obj instanceof Map)) {
                        report.add("[ERROR] Item #" + index + " no es un mapa; será ignorado.");
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    int slot = safeInt(map.get("slot"), -1);
                    if (slot < 0 || slot >= invSize) {
                        report.add("[ERROR] Item #" + index + ": slot fuera de rango (" + slot + ") para inventario de " + invSize + ".");
                    } else {
                        if (!seenSlots.add(slot)) {
                            report.add("[WARN] Item #" + index + ": slot duplicado (" + slot + "). Se sobrescribirá.");
                        }
                    }
                    String materialName = String.valueOf(map.getOrDefault("material", "STONE"));
                    Material material = normalizeMaterial(materialName);
                    if (material == null) {
                        report.add("[WARN] Item #" + index + ": material inválido '" + materialName + "'; se usará STONE.");
                    }
                    Map<String, Object> actionMap = toStringObjectMap(map.get("action"));
                    String typeStr = String.valueOf(actionMap.getOrDefault("type", "command"));
                    MenuActionType type = MenuActionType.from(typeStr);
                    String value = String.valueOf(actionMap.getOrDefault("value", ""));
                    if (value.trim().isEmpty() && type != MenuActionType.MESSAGE) {
                        report.add("[WARN] Item #" + index + ": acción '" + typeStr + "' sin valor.");
                    } else {
                        report.add("[OK] Item #" + index + ": acción '" + typeStr + "' configurada.");
                    }
                }
            }
        }

        return report;
    }

    private void buildMenus() {
        inventories.clear();
        menuActions.clear();
        menuTitles.clear();
        titleToId.clear();
        menuOpenPermissions.clear();

        // Primero intentar cargar desde archivos separados en menus/
        File menusDir = new File(plugin.getDataFolder(), "menus");
        if (!menusDir.exists()) {
            // Crear carpeta y semillar recursos por defecto si están en el JAR
            menusDir.mkdirs();
            if (plugin instanceof JavaPlugin) {
                JavaPlugin jp = (JavaPlugin) plugin;
                String[] defaults = new String[] {
                    "menus/main.yml",
                    "menus/info.yml",
                    "menus/shop.yml",
                    "menus/admin.yml"
                };
                for (String res : defaults) {
                    try {
                        jp.saveResource(res, false);
                    } catch (IllegalArgumentException ignored) {
                        // El recurso no existe en el JAR; ignorar
                    } catch (Exception e) {
                        plugin.getLogger().warning("No se pudo guardar recurso '" + res + "': " + e.getMessage());
                    }
                }
            }
        }
        boolean loadedFromFiles = false;
        
        if (menusDir.exists() && menusDir.isDirectory()) {
            File[] menuFiles = menusDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (menuFiles != null && menuFiles.length > 0) {
                loadedFromFiles = true;
                plugin.getLogger().info("Cargando menús desde archivos separados en menus/");
                
                for (File menuFile : menuFiles) {
                    String menuId = menuFile.getName().replace(".yml", "");
                    try {
                        YamlConfiguration menuConfig = YamlConfiguration.loadConfiguration(menuFile);
                        buildSingleMenuFromFile(menuId, menuConfig);
                        plugin.getLogger().info("Menú '" + menuId + "' cargado desde " + menuFile.getName());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error cargando menú desde " + menuFile.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        // Si no se cargaron archivos, usar el método original (config.yml)
        if (!loadedFromFiles) {
            plugin.getLogger().info("No se encontraron archivos de menú separados, usando config.yml");
            ConfigurationSection menusSection = plugin.getConfig().getConfigurationSection("menus");
            if (menusSection != null) {
                for (String id : menusSection.getKeys(false)) {
                    ConfigurationSection def = menusSection.getConfigurationSection(id);
                    if (def == null) {
                        plugin.getLogger().warning("Menú '" + id + "' inválido: sección nula.");
                        continue;
                    }
                    buildSingleMenu(id, def);
                }
            } else {
                // Legacy: construir menú único 'main'
                Map<String, Object> def = new HashMap<>();
                def.put("title", plugin.getConfig().getString("menu.title", "Menu"));
                def.put("size", plugin.getConfig().getInt("menu.size", 27));
                Object items = plugin.getConfig().getList("menu.items");
                def.put("items", items == null ? new ArrayList<>() : items);
                buildSingleMenu("main", def);
            }
        }
    }

    private void buildSingleMenu(String id, Map<String, Object> def) {
        String rawTitle = String.valueOf(def.getOrDefault("title", "Menu"));
        String title = resolveTitle(rawTitle);
        int size = safeInt(def.get("size"), 27);

        Inventory inv = Bukkit.createInventory(null, size, language.color(title));
        inventories.put(id, inv);
        menuTitles.put(id, title);
        titleToId.put(language.color(title), id);

        Map<Integer, MenuAction> actions = new HashMap<>();
        menuActions.put(id, actions);

        List<?> itemsRaw = toList(def.get("items"));
        // Permiso opcional para abrir este menú
        String openPermMap = String.valueOf(def.getOrDefault("permission_open", ""));
        if (openPermMap == null) openPermMap = "";
        menuOpenPermissions.put(id, openPermMap);
        int invSize = inv.getSize();
        for (Object obj : itemsRaw) {
            if (!(obj instanceof Map)) {
                plugin.getLogger().warning("Elemento de 'menus." + id + ".items' ignorado: se esperaba un mapa.");
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;

            int slot = safeInt(map.get("slot"), 0);
            if (slot < 0 || slot >= invSize) {
                plugin.getLogger().warning("Menú '" + id + "': slot fuera de rango (" + slot + ")");
                continue;
            }

            ItemStack stack = buildItemStack(map);
            inv.setItem(slot, stack);

            Map<String, Object> actionMap = toStringObjectMap(map.get("action"));
            MenuActionType type = MenuActionType.from(String.valueOf(actionMap.getOrDefault("type", "command")));
            String value = String.valueOf(actionMap.getOrDefault("value", ""));
            // Permiso opcional por ítem
            String itemPermMap = String.valueOf(map.getOrDefault("permission", ""));
            if (itemPermMap == null) itemPermMap = "";
            actions.put(slot, new MenuAction(type, value, itemPermMap));
        }

        // Borde configurable: leer override de menú, con fallback global
        boolean defaultBorderEnabled = plugin.getConfig().getBoolean("border.enabled", true);
        String defaultBorderMaterialName = plugin.getConfig().getString("border.material", "BLACK_STAINED_GLASS_PANE");
        String defaultBorderName = plugin.getConfig().getString("border.name", " ");
        Material defaultBorderMaterial = normalizeMaterial(defaultBorderMaterialName);
        if (defaultBorderMaterial == null) defaultBorderMaterial = normalizeMaterial("BLACK_STAINED_GLASS_PANE");

        boolean borderEnabled = defaultBorderEnabled;
        Material borderMaterial = defaultBorderMaterial;
        String borderName = defaultBorderName;

        // Legacy schema 'menu.border' override, if present
        ConfigurationSection legacyMenuSec = plugin.getConfig().getConfigurationSection("menu");
        ConfigurationSection legacyBorderSec = legacyMenuSec != null ? legacyMenuSec.getConfigurationSection("border") : null;
        if (legacyBorderSec != null) {
            borderEnabled = legacyBorderSec.getBoolean("enabled", borderEnabled);
            String matName = legacyBorderSec.getString("material", borderMaterial.name());
            Material m = normalizeMaterial(matName);
            if (m != null) borderMaterial = m;
            borderName = legacyBorderSec.getString("name", borderName);
        }

        if (borderEnabled) {
            fillBorderWithPane(inv, borderMaterial, borderName);
        }

        // Botón volver configurable: global y override por menú
        boolean defaultBackEnabled = plugin.getConfig().getBoolean("back_button.enabled", false);
        String defaultBackMaterialName = plugin.getConfig().getString("back_button.material", "ARROW");
        String defaultBackName = plugin.getConfig().getString("back_button.name", "&7Volver");
        int defaultBackSlot = plugin.getConfig().getInt("back_button.slot", invSize - 9); // primera fila inferior por defecto

        boolean backEnabled = defaultBackEnabled;
        Material backMaterial = normalizeMaterial(defaultBackMaterialName);
        if (backMaterial == null) backMaterial = Material.ARROW;
        String backName = defaultBackName;
        int backSlot = Math.max(0, Math.min(invSize - 1, defaultBackSlot));

        Map<String, Object> backOverride = toStringObjectMap(((Map<String, Object>)def).get("back_button"));
        if (!backOverride.isEmpty()) {
            backEnabled = Boolean.parseBoolean(String.valueOf(backOverride.getOrDefault("enabled", backEnabled)));
            String bm = String.valueOf(backOverride.getOrDefault("material", backMaterial.name()));
            Material m = normalizeMaterial(bm);
            if (m != null) backMaterial = m;
            backName = String.valueOf(backOverride.getOrDefault("name", backName));
            backSlot = safeInt(backOverride.getOrDefault("slot", backSlot), backSlot);
            backSlot = Math.max(0, Math.min(invSize - 1, backSlot));
        }

        if (backEnabled) {
            ItemStack existing = inv.getItem(backSlot);
            boolean canReplace = false;
            if (existing == null || existing.getType() == Material.AIR) {
                canReplace = true;
            } else {
                // Permitir reemplazar borde si coincide material y nombre
                if (existing.getType() == borderMaterial) {
                    ItemMeta em = existing.getItemMeta();
                    String existingName = (em != null && em.hasDisplayName()) ? em.getDisplayName() : null;
                    String borderDisplay = language.color(resolveString(borderName == null ? " " : borderName));
                    if (borderDisplay.equals(existingName)) {
                        canReplace = true;
                    }
                }
            }

            if (canReplace) {
                ItemStack backItem = new ItemStack(backMaterial, 1);
                ItemMeta meta = backItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(language.color(resolveString(backName)));
                    backItem.setItemMeta(meta);
                }
                inv.setItem(backSlot, backItem);
                actions.put(backSlot, new MenuAction(MenuActionType.BACK_MENU, ""));
            } else {
                plugin.getLogger().info("Slot de botón volver ocupado en '" + id + "' (" + backSlot + "), no se inserta automáticamente.");
            }
        }
    }

    private void buildSingleMenu(String id, ConfigurationSection def) {
        String rawTitle = def.getString("title", "Menu");
        String title = resolveTitle(rawTitle);
        int size = def.getInt("size", 27);

        Inventory inv = Bukkit.createInventory(null, size, language.color(title));
        inventories.put(id, inv);
        menuTitles.put(id, title);
        titleToId.put(language.color(title), id);

        Map<Integer, MenuAction> actions = new HashMap<>();
        menuActions.put(id, actions);

        List<?> itemsRaw = def.getList("items");
        if (itemsRaw == null) itemsRaw = new ArrayList<>();
        // Permiso opcional para abrir este menú
        String openPermSec = def.getString("permission_open", "");
        if (openPermSec == null) openPermSec = "";
        menuOpenPermissions.put(id, openPermSec);
        int invSize = inv.getSize();
        for (Object obj : itemsRaw) {
            Map<String, Object> map = toStringObjectMap(obj);
            if (map.isEmpty()) {
                plugin.getLogger().warning("Elemento de 'menus." + id + ".items' ignorado: se esperaba un mapa.");
                continue;
            }

            int slot = safeInt(map.get("slot"), 0);
            if (slot < 0 || slot >= invSize) {
                plugin.getLogger().warning("Menú '" + id + "': slot fuera de rango (" + slot + ")");
                continue;
            }

            ItemStack stack = buildItemStack(map);
            inv.setItem(slot, stack);

            Map<String, Object> actionMap = toStringObjectMap(map.get("action"));
            MenuActionType type = MenuActionType.from(String.valueOf(actionMap.getOrDefault("type", "command")));
            String value = String.valueOf(actionMap.getOrDefault("value", ""));
            String itemPermSec = String.valueOf(map.getOrDefault("permission", ""));
            if (itemPermSec == null) itemPermSec = "";
            actions.put(slot, new MenuAction(type, value, itemPermSec));
        }

        // Borde configurable: leer override por menú, con fallback global
        boolean defaultBorderEnabled = plugin.getConfig().getBoolean("border.enabled", true);
        String defaultBorderMaterialName = plugin.getConfig().getString("border.material", "BLACK_STAINED_GLASS_PANE");
        String defaultBorderName = plugin.getConfig().getString("border.name", " ");
        Material defaultBorderMaterial = normalizeMaterial(defaultBorderMaterialName);
        if (defaultBorderMaterial == null) defaultBorderMaterial = normalizeMaterial("BLACK_STAINED_GLASS_PANE");

        boolean borderEnabled = defaultBorderEnabled;
        Material borderMaterial = defaultBorderMaterial;
        String borderName = defaultBorderName;

        ConfigurationSection borderSec = def.getConfigurationSection("border");
        if (borderSec != null) {
            borderEnabled = borderSec.getBoolean("enabled", borderEnabled);
            String matName = borderSec.getString("material", borderMaterial.name());
            Material m = normalizeMaterial(matName);
            if (m != null) borderMaterial = m;
            borderName = borderSec.getString("name", borderName);
        } else {
            // Legacy fallback: 'menu.border' if present
            ConfigurationSection legacyMenuSec = plugin.getConfig().getConfigurationSection("menu");
            ConfigurationSection legacyBorderSec = legacyMenuSec != null ? legacyMenuSec.getConfigurationSection("border") : null;
            if (legacyBorderSec != null) {
                borderEnabled = legacyBorderSec.getBoolean("enabled", borderEnabled);
                String matName = legacyBorderSec.getString("material", borderMaterial.name());
                Material m = normalizeMaterial(matName);
                if (m != null) borderMaterial = m;
                borderName = legacyBorderSec.getString("name", borderName);
            }
        }

        if (borderEnabled) {
            fillBorderWithPane(inv, borderMaterial, borderName);
        }

        // Botón volver configurable: global y override por menú
        boolean defaultBackEnabled = plugin.getConfig().getBoolean("back_button.enabled", false);
        String defaultBackMaterialName = plugin.getConfig().getString("back_button.material", "ARROW");
        String defaultBackName = plugin.getConfig().getString("back_button.name", "&7Volver");
        int defaultBackSlot = plugin.getConfig().getInt("back_button.slot", invSize - 9);

        boolean backEnabled = defaultBackEnabled;
        Material backMaterial = normalizeMaterial(defaultBackMaterialName);
        if (backMaterial == null) backMaterial = Material.ARROW;
        String backName = defaultBackName;
        int backSlot = Math.max(0, Math.min(invSize - 1, defaultBackSlot));

        ConfigurationSection backSec = def.getConfigurationSection("back_button");
        if (backSec != null) {
            backEnabled = backSec.getBoolean("enabled", backEnabled);
            String bm = backSec.getString("material", backMaterial.name());
            Material m = normalizeMaterial(bm);
            if (m != null) backMaterial = m;
            backName = backSec.getString("name", backName);
            backSlot = Math.max(0, Math.min(invSize - 1, backSec.getInt("slot", backSlot)));
        }

        if (backEnabled) {
            ItemStack existing = inv.getItem(backSlot);
            boolean canReplace = false;
            if (existing == null || existing.getType() == Material.AIR) {
                canReplace = true;
            } else {
                // Permitir reemplazar borde si coincide material y nombre
                if (existing.getType() == borderMaterial) {
                    ItemMeta em = existing.getItemMeta();
                    String existingName = (em != null && em.hasDisplayName()) ? em.getDisplayName() : null;
                    String borderDisplay = language.color(resolveString(borderName == null ? " " : borderName));
                    if (borderDisplay.equals(existingName)) {
                        canReplace = true;
                    }
                }
            }

            if (canReplace) {
                ItemStack backItem = new ItemStack(backMaterial, 1);
                ItemMeta meta = backItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(language.color(resolveString(backName)));
                    backItem.setItemMeta(meta);
                }
                inv.setItem(backSlot, backItem);
                actions.put(backSlot, new MenuAction(MenuActionType.BACK_MENU, ""));
            } else {
            plugin.getLogger().info("Slot de botón volver ocupado en '" + id + "' (" + backSlot + "), no se inserta automáticamente.");
            }
        }
    }

    private ItemStack buildItemStack(Map<String, Object> map) {
        String materialName = String.valueOf(map.getOrDefault("material", "STONE"));
        Material material = normalizeMaterial(materialName);
        if (material == null) material = Material.STONE;

        // Construcción inteligente para compatibilidad 1.8↔1.13+
        ItemStack stack;
        String matNameUpper = materialName == null ? "STONE" : materialName.trim().toUpperCase();
        // Soporte de vidrios teñidos en 1.8
        if (material.name().equals("STAINED_GLASS_PANE") && matNameUpper.endsWith("_STAINED_GLASS_PANE")) {
            short data = dyeDataFromName(matNameUpper.replace("_STAINED_GLASS_PANE", ""));
            stack = itemWithData(material, 1, data);
        } else if ((matNameUpper.equals("HEAD") || matNameUpper.equals("PLAYER_HEAD") || matNameUpper.equals("SKULL"))
                && (Material.matchMaterial("PLAYER_HEAD") == null)) {
            // En 1.8 usar SKULL_ITEM con data 3 (cabeza de jugador)
            Material skull = Material.matchMaterial("SKULL_ITEM");
            if (skull != null) {
                stack = itemWithData(skull, 1, 3);
            } else {
                stack = new ItemStack(material);
            }
        } else {
            stack = new ItemStack(material);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = String.valueOf(map.getOrDefault("name", ""));
            meta.setDisplayName(language.color(resolveString(name)));

            Object loreObj = map.get("lore");
            if (loreObj instanceof List) {
                List<String> coloredLore = new ArrayList<>();
                for (Object l : (List<?>) loreObj) {
                    coloredLore.add(language.color(resolveString(String.valueOf(l))));
                }
                meta.setLore(coloredLore);
            }

            // Soporte de cabezas: owner y textura
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) meta;
                String owner = String.valueOf(map.getOrDefault("head_owner", String.valueOf(map.getOrDefault("head", ""))));
                if (owner != null && !owner.trim().isEmpty()) {
                    org.bukkit.OfflinePlayer offlinePlayer = null;
                    try {
                        java.util.UUID uuid = java.util.UUID.fromString(owner.trim());
                        offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                    } catch (IllegalArgumentException ignored) {
                        offlinePlayer = resolveOfflinePlayerByName(owner.trim());
                    }
                    if (offlinePlayer != null) {
                        skullMeta.setOwningPlayer(offlinePlayer);
                    }
                }
                // Aceptar Base64 directa o URL de textures.minecraft.net
                String texture = String.valueOf(map.getOrDefault("head_texture", String.valueOf(map.getOrDefault("texture", ""))));
                String textureUrl = String.valueOf(map.getOrDefault("head_texture_url", String.valueOf(map.getOrDefault("texture_url", ""))));
                if (textureUrl != null && !textureUrl.trim().isEmpty()) {
                    String base64 = encodeTextureUrlToBase64(textureUrl.trim());
                    if (base64 != null) applyHeadTexture(skullMeta, base64);
                } else if (texture != null && !texture.trim().isEmpty()) {
                    String t = texture.trim();
                    if (t.startsWith("http://") || t.startsWith("https://")) {
                        String base64 = encodeTextureUrlToBase64(t);
                        if (base64 != null) applyHeadTexture(skullMeta, base64);
                    } else {
                        applyHeadTexture(skullMeta, t);
                    }
                }
                meta = skullMeta;
            }

            stack.setItemMeta(meta);
        }
        return stack;
    }

    // Construye ItemStack y aplica data/durabilidad de forma compatible 1.8↔1.21.8
    private ItemStack itemWithData(Material material, int amount, int data) {
        ItemStack stack = new ItemStack(material, amount);
        // Intentar API moderna Damageable
        try {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                Class<?> dmg = Class.forName("org.bukkit.inventory.meta.Damageable");
                if (dmg.isInstance(meta)) {
                    java.lang.reflect.Method setDamage = dmg.getMethod("setDamage", int.class);
                    setDamage.invoke(meta, data);
                    stack.setItemMeta(meta);
                    return stack;
                }
            }
        } catch (Throwable ignored) {}

        // Fallback legacy: usar setDurability(short) por reflexión para evitar deprecación en compilación
        try {
            java.lang.reflect.Method setDurability = ItemStack.class.getMethod("setDurability", short.class);
            setDurability.invoke(stack, (short) data);
        } catch (Throwable ignored) {}
        return stack;
    }

    // Helper methods to build items from material and name
    private ItemStack createItemStack(String materialName, String name, Object unused1, Object unused2) {
        Map<String, Object> map = new HashMap<>();
        map.put("material", materialName == null ? "STONE" : materialName);
        map.put("name", name == null ? "" : name);
        return buildItemStack(map);
    }

    private ItemStack createItem(String materialName, String name, Object unused1, Object unused2) {
        return createItemStack(materialName, name, unused1, unused2);
    }

    private String encodeTextureUrlToBase64(String url) {
        try {
            if (url == null || url.trim().isEmpty()) return null;
            String clean = url.trim();
            // Validación ligera: debe contener "textures.minecraft.net/texture/"
            if (!clean.contains("textures.minecraft.net/texture/")) {
                // Permitir URLs personalizadas de skins, pero advertir
                plugin.getLogger().warning("URL de textura no parece de textures.minecraft.net: " + clean);
            }
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + clean + "\"}}}";
            return java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo codificar URL de textura: " + e.getMessage());
            return null;
        }
    }

    private org.bukkit.OfflinePlayer resolveOfflinePlayerByName(String name) {
        if (name == null) return null;
        String n = name.trim();
        if (n.isEmpty()) return null;
        // Prefer exact online player
        Player online = Bukkit.getPlayerExact(n);
        if (online != null) return online;
        // Try Paper's cached lookup if available
        try {
            java.lang.reflect.Method m = org.bukkit.Bukkit.class.getMethod("getOfflinePlayerIfCached", String.class);
            Object result = m.invoke(null, n);
            if (result instanceof org.bukkit.OfflinePlayer) {
                return (org.bukkit.OfflinePlayer) result;
            }
        } catch (NoSuchMethodException ignored) {
            // Method not present on Spigot
        } catch (Exception e) {
            // Ignore and continue to next strategy
        }
        // If server runs in offline mode, derive offline UUID from name
        try {
            if (!org.bukkit.Bukkit.getServer().getOnlineMode()) {
                java.util.UUID offlineUuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + n).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return org.bukkit.Bukkit.getOfflinePlayer(offlineUuid);
            }
        } catch (Exception ignored) {
        }
        // No safe resolution available
        return null;
    }

    private Material normalizeMaterial(String name) {
        if (name == null) return null;
        String s = name.trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("minecraft:")) s = s.substring("minecraft:".length());
        // Aliases comunes para cabezas
        if (s.equalsIgnoreCase("HEAD") || s.equalsIgnoreCase("PLAYER_HEAD") || s.equalsIgnoreCase("SKULL")) {
            Material m = Material.matchMaterial("PLAYER_HEAD");
            if (m != null) return m;
            // Fallback 1.8
            m = Material.matchMaterial("SKULL_ITEM");
            if (m == null) m = Material.matchMaterial("SKULL");
            if (m != null) return m;
        }
        // Fallback de colores 1.13+ → 1.8 para vidrios teñidos
        if (s.toUpperCase().endsWith("_STAINED_GLASS_PANE")) {
            Material m = Material.matchMaterial(s);
            if (m != null) return m; // 1.13+
            m = Material.matchMaterial("STAINED_GLASS_PANE"); // 1.8
            if (m != null) return m;
        }
        Material material = Material.matchMaterial(s);
        return material;
    }

    private void applyHeadTexture(ItemMeta meta, String texture) {
        // Primero intentar con API moderna (Spigot/Paper), luego caer a authlib por reflexión
        try {
            // Limpiar owner previo si existe para evitar que sobrescriba la textura
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                try {
                    org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) meta;
                    sm.setOwningPlayer(null);
                } catch (Throwable ignored) {
                    // Intentar método legacy setOwner(String)
                    try {
                        java.lang.reflect.Method setOwner = meta.getClass().getMethod("setOwner", String.class);
                        setOwner.invoke(meta, new Object[]{null});
                    } catch (Throwable ignored2) {}
                }
            }
            // Soportar tanto Bukkit moderno como Paper legacy
            Class<?> playerProfileClass;
            Class<?> profilePropertyClass;
            String profileApi = "";
            try {
                playerProfileClass = Class.forName("org.bukkit.profile.PlayerProfile");
                profilePropertyClass = Class.forName("org.bukkit.profile.ProfileProperty");
                profileApi = "bukkit";
            } catch (ClassNotFoundException eBukkit) {
                playerProfileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
                profilePropertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
                profileApi = "paper";
            }

            java.lang.reflect.Method createProfile = org.bukkit.Bukkit.class.getMethod("createProfile", java.util.UUID.class, String.class);
            Object profile = createProfile.invoke(null, java.util.UUID.randomUUID(), "CrMenuHead");

            // Crear propiedad de textura; intentar ctor (String, String) y si no existe, (String, String, String)
            Object texturesProp;
            try {
                java.lang.reflect.Constructor<?> propCtor = profilePropertyClass.getConstructor(String.class, String.class);
                texturesProp = propCtor.newInstance("textures", texture);
            } catch (NoSuchMethodException nsme) {
                java.lang.reflect.Constructor<?> propCtor3 = profilePropertyClass.getConstructor(String.class, String.class, String.class);
                texturesProp = propCtor3.newInstance("textures", texture, null);
            }

            // Usar PlayerProfile#setProperty(ProfileProperty) o, si no, agregar vía getProperties().add(ProfileProperty)
            try {
                java.lang.reflect.Method setProperty = playerProfileClass.getMethod("setProperty", profilePropertyClass);
                setProperty.invoke(profile, texturesProp);
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException nsmeSet) {
                // Fallback: getProperties().add(ProfileProperty)
                try {
                    java.lang.reflect.Method getProperties = playerProfileClass.getMethod("getProperties");
                    Object props = getProperties.invoke(profile);
                    java.lang.reflect.Method addMethod = props.getClass().getMethod("add", profilePropertyClass);
                    addMethod.invoke(props, texturesProp);
                } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException nsmeAdd) {
                    // No se pudo establecer propiedad por rutas disponibles
                    plugin.getLogger().warning("No se pudo agregar propiedad de textura al PlayerProfile: " + nsmeAdd.getClass().getSimpleName());
                }
            }

            // Establecer el perfil en SkullMeta intentando vía la interfaz para máxima compatibilidad
            try {
                Class<?> skullMetaInterface = Class.forName("org.bukkit.inventory.meta.SkullMeta");
                try {
                    java.lang.reflect.Method setPlayerProfile = skullMetaInterface.getMethod("setPlayerProfile", playerProfileClass);
                    setPlayerProfile.invoke(meta, profile);
                    plugin.getLogger().info("Textura aplicada vía setPlayerProfile(" + profileApi + ") len=" + (texture != null ? texture.length() : 0));
                } catch (NoSuchMethodException nsme2) {
                    // Algunas builds exponen setProfile(PlayerProfile)
                    java.lang.reflect.Method setProfileMethod = skullMetaInterface.getMethod("setProfile", playerProfileClass);
                    setProfileMethod.invoke(meta, profile);
                    plugin.getLogger().info("Textura aplicada vía setProfile(" + profileApi + ") len=" + (texture != null ? texture.length() : 0));
                }
            } catch (ClassNotFoundException ignored) {
                // No disponible
            }
            return; // aplicado correctamente por API moderna
        } catch (ClassNotFoundException ignored) {
            // API moderna no disponible, continuar con authlib
        } catch (NoSuchMethodException ignored) {
            // Método no disponible, continuar con authlib
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getTargetException();
            plugin.getLogger().warning("No se pudo aplicar textura de cabeza (API moderna): " + (cause != null ? cause.getClass().getSimpleName() + ": " + cause.getMessage() : ite.getMessage()));
            // Intentar fallback
        } catch (Exception e) {
            // Otros errores de API moderna, intentar fallback
        }

        // Fallback: usar com.mojang.authlib.GameProfile por reflexión (CraftMetaSkull#profile)
        try {
            Class<?> skullMetaClass = meta.getClass();
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Class<?> propertyMapClass = Class.forName("com.mojang.authlib.properties.PropertyMap");

            java.util.UUID uuid = java.util.UUID.randomUUID();
            Object profile = gameProfileClass.getConstructor(java.util.UUID.class, String.class).newInstance(uuid, "CrMenuHead");
            Object props = gameProfileClass.getMethod("getProperties").invoke(profile);
            java.lang.reflect.Constructor<?> propCtor = propertyClass.getConstructor(String.class, String.class);
            Object texturesProp = propCtor.newInstance("textures", texture);
            // Intentar PropertyMap#put(String, Property)
            try {
                java.lang.reflect.Method putMethod = propertyMapClass.getMethod("put", String.class, propertyClass);
                putMethod.invoke(props, "textures", texturesProp);
            } catch (NoSuchMethodException ex) {
                // Intentar método genérico put(Object, Object)
                try {
                    java.lang.reflect.Method putGeneric = props.getClass().getMethod("put", Object.class, Object.class);
                    putGeneric.invoke(props, "textures", texturesProp);
                } catch (NoSuchMethodException ex2) {
                    // No hay forma de insertar la propiedad
                    plugin.getLogger().warning("No se pudo insertar propiedad de textura en PropertyMap: " + ex2.getMessage());
                }
            }

            // Establecer el GameProfile en SkullMeta solo si el campo es compatible
            try {
                java.lang.reflect.Field profileField = skullMetaClass.getDeclaredField("profile");
                profileField.setAccessible(true);
                Class<?> fieldType = profileField.getType();
                if (fieldType.isAssignableFrom(gameProfileClass)) {
                    profileField.set(meta, profile);
                } else {
                    // En versiones modernas el campo es ResolvableProfile: evitar escribir tipo incompatible
                    plugin.getLogger().info("Campo 'profile' no compatible (" + fieldType.getName() + ") para GameProfile; se omite fallback.");
                }
            } catch (NoSuchFieldException nsfe) {
                // Intentar métodos antiguos si existen
                try {
                    java.lang.reflect.Method setOwnerProfile = skullMetaClass.getMethod("setOwnerProfile", gameProfileClass);
                    setOwnerProfile.invoke(meta, profile);
                } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {
                    // No hay ruta válida
                }
            }
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getTargetException();
            plugin.getLogger().warning("No se pudo aplicar textura de cabeza (authlib): " + (cause != null ? cause.getClass().getSimpleName() + ": " + cause.getMessage() : ite.getMessage()));
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo aplicar textura de cabeza (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    private void fillBorderWithPane(Inventory inv, Material material, String displayName) {
        if (inv == null) return;
        int size = inv.getSize();
        if (size <= 0 || size % 9 != 0) return;
        int rows = size / 9;
        String matName = (material != null ? material.name() : "BLACK_STAINED_GLASS_PANE");
        ItemStack pane = createItemStack(matName, displayName == null ? " " : displayName, null, null);
        // Top row
        for (int i = 0; i < 9; i++) placeIfEmpty(inv, i, pane);
        // Bottom row
        int bottomStart = (rows - 1) * 9;
        for (int i = 0; i < 9; i++) placeIfEmpty(inv, bottomStart + i, pane);
        // Left and right borders for middle rows
        for (int r = 1; r < rows - 1; r++) {
            placeIfEmpty(inv, r * 9, pane);
            placeIfEmpty(inv, r * 9 + 8, pane);
        }
    }

    // Mapeo de colores de tinte a data values (1.8)
    private short dyeDataFromName(String colorName) {
        String c = colorName.toUpperCase();
        switch (c) {
            case "WHITE": return 0;
            case "ORANGE": return 1;
            case "MAGENTA": return 2;
            case "LIGHT_BLUE": return 3;
            case "YELLOW": return 4;
            case "LIME": return 5;
            case "PINK": return 6;
            case "GRAY": return 7;
            case "LIGHT_GRAY": case "SILVER": return 8;
            case "CYAN": return 9;
            case "PURPLE": return 10;
            case "BLUE": return 11;
            case "BROWN": return 12;
            case "GREEN": return 13;
            case "RED": return 14;
            case "BLACK": return 15;
            default: return 15; // negro por defecto
        }
    }

    private void placeIfEmpty(Inventory inv, int slot, org.bukkit.inventory.ItemStack item) {
        try {
            org.bukkit.inventory.ItemStack existing = inv.getItem(slot);
            if (existing == null || existing.getType() == org.bukkit.Material.AIR) {
                inv.setItem(slot, item.clone());
            }
        } catch (Exception ignored) {
        }
    }

    private String resolveString(String raw) {
        if (raw == null) return "";
        raw = applyPlaceholders(raw);
        if (raw.startsWith("@lang:")) {
            String key = raw.substring("@lang:".length());
            return language.get(key);
        }
        return raw;
    }

    // Resolver específicamente títulos de menú, forzando el fallback a "Menu"
    private String resolveTitle(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Menu";
        String s = raw.trim();
        // Si referencia la clave de idioma de título, usar fijo "Menu"
        if (s.equalsIgnoreCase("@lang:menu_title")) return "Menu";
        // En otros casos permitir resolución estándar (placeholders y @lang genérico)
        return resolveString(s);
    }

    private String applyPlaceholders(String s) {
        if (s == null) return "";
        return s.replace("{prefix}", language.get("prefix"));
    }

    private String applyPlaceholders(Player player, String s) {
        if (s == null) return "";
        String out = s.replace("{prefix}", language.get("prefix"));
        int ping = -1;
        try {
            java.lang.reflect.Method getPing = player.getClass().getMethod("getPing");
            Object p = getPing.invoke(player);
            if (p != null) ping = Integer.parseInt(String.valueOf(p));
        } catch (Exception e1) {
            try {
                Object spigot = player.getClass().getMethod("spigot").invoke(player);
                java.lang.reflect.Method spigotGetPing = spigot.getClass().getMethod("getPing");
                Object p2 = spigotGetPing.invoke(spigot);
                if (p2 != null) ping = Integer.parseInt(String.valueOf(p2));
            } catch (Exception ignored) {
            }
        }
        if (ping >= 0) {
            out = out.replace("%player_ping%", String.valueOf(ping));
            out = out.replace("%ping%", String.valueOf(ping));
        }
        // Integración opcional con PlaceholderAPI si está presente
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method setPlaceholders = papiClass.getMethod("setPlaceholders", org.bukkit.entity.Player.class, String.class);
            Object result = setPlaceholders.invoke(null, player, out);
            if (result instanceof String) {
                out = (String) result;
            }
        } catch (ClassNotFoundException ignored) {
            // PlaceholderAPI no instalado; usar solo reemplazos internos
        } catch (Throwable t) {
            // Cualquier error en integración PAPI se ignora para no romper
        }
        return out;
    }

    private Map<String, Object> toStringObjectMap(Object obj) {
        Map<String, Object> out = new HashMap<>();
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> raw = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                Object key = entry.getKey();
                if (key != null) {
                    out.put(String.valueOf(key), entry.getValue());
                }
            }
        } else if (obj instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection) obj;
            for (String key : section.getKeys(false)) {
                out.put(key, section.get(key));
            }
        }
        return out;
    }

    private List<?> toList(Object obj) {
        if (obj instanceof List<?>) return (List<?>) obj;
        return new ArrayList<>();
    }

    private int safeInt(Object obj, int def) {
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (Exception e) {
            return def;
        }
    }

    private String getDefaultMenuId() {
        // Primero verificar si hay menús cargados desde archivos separados
        if (!inventories.isEmpty()) {
            if (inventories.containsKey("main")) return "main";
            return inventories.keySet().iterator().next();
        }
        
        // Si no hay menús cargados, verificar config.yml
        ConfigurationSection menusSection = plugin.getConfig().getConfigurationSection("menus");
        if (menusSection != null) {
            if (menusSection.getKeys(false).contains("main")) return "main";
            java.util.Set<String> keys = menusSection.getKeys(false);
            if (!keys.isEmpty()) return keys.iterator().next();
        }
        return "main"; // legacy
    }

    enum MenuActionType {
        COMMAND_PLAYER,
        COMMAND_CONSOLE,
        MESSAGE,
        OPEN_MENU,
        BACK_MENU;

        static MenuActionType from(String s) {
            if (s == null) return COMMAND_PLAYER;
            switch (s.toLowerCase()) {
                case "command":
                case "command_player":
                    return COMMAND_PLAYER;
                case "command_console":
                case "console":
                    return COMMAND_CONSOLE;
                case "message":
                case "msg":
                    return MESSAGE;
                case "open_menu":
                case "menu":
                    return OPEN_MENU;
                case "back_menu":
                case "back":
                    return BACK_MENU;
                default:
                    return COMMAND_PLAYER;
            }
        }
    }

    static class MenuAction {
        final MenuActionType type;
        final String value;
        final String permission; // opcional por ítem

        MenuAction(MenuActionType type, String value) {
            this.type = type;
            this.value = value;
            this.permission = "";
        }

        MenuAction(MenuActionType type, String value, String permission) {
            this.type = type;
            this.value = value;
            this.permission = permission == null ? "" : permission;
        }
    }

    // Método para construir menú desde archivo YAML separado
    private void buildSingleMenuFromFile(String id, YamlConfiguration config) {
        String rawTitle = config.getString("title", "Menu");
        String title = resolveTitle(rawTitle);
        int size = config.getInt("size", 27);
        
        // Leer permission_open si existe
        String permissionOpen = config.getString("permission_open", "");
        if (!permissionOpen.isEmpty()) {
            menuOpenPermissions.put(id, permissionOpen);
        }

        // Crear inventario
        Inventory inv = Bukkit.createInventory(null, size, language.color(title));
        inventories.put(id, inv);
        menuTitles.put(id, title);
        titleToId.put(language.color(title), id);

        // Initialize actions map for this menu id
        Map<Integer, MenuAction> actions = new HashMap<>();
        menuActions.put(id, actions);

        // Configurar borde si existe
        ConfigurationSection borderSection = config.getConfigurationSection("border");
        if (borderSection != null) {
            boolean enabled = borderSection.getBoolean("enabled", false);
            if (enabled) {
                String materialName = borderSection.getString("material", "GRAY_STAINED_GLASS_PANE");
                String name = borderSection.getString("name", " ");
                ItemStack borderItem = createItemStack(materialName, name, null, null);
                
                // Llenar bordes
                for (int i = 0; i < size; i++) {
                    if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                        inv.setItem(i, borderItem);
                    }
                }
            }
        }

        // Configurar botón de atrás si existe
        ConfigurationSection backSection = config.getConfigurationSection("back_button");
        if (backSection != null) {
            boolean enabled = backSection.getBoolean("enabled", false);
            if (enabled) {
                int slot = backSection.getInt("slot", size - 1);
                String materialName = backSection.getString("material", "ARROW");
                String name = backSection.getString("name", "@lang:back_button");
                ItemStack backItem = createItem(materialName, name, null, null);
                inv.setItem(slot, backItem);
                
                String targetMenu = backSection.getString("target_menu", "main");
                actions.put(slot, new MenuAction(MenuActionType.OPEN_MENU, targetMenu));
            }
        }

        // Procesar items
        List<?> itemsList = config.getList("items");
        if (itemsList != null) {
            for (Object itemObj : itemsList) {
                if (itemObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) itemObj;
                    buildSingleMenuItem(id, inv, itemMap);
                }
            }
        }
    }
    private void buildSingleMenuItem(String id, Inventory inv, Map<String, Object> map) {
        int invSize = inv.getSize();
        int slot = safeInt(map.get("slot"), 0);
        if (slot < 0 || slot >= invSize) {
            plugin.getLogger().warning("Menú '" + id + "': slot fuera de rango (" + slot + ")");
            return;
        }

        ItemStack stack = buildItemStack(map);
        inv.setItem(slot, stack);

        Map<String, Object> actionMap = toStringObjectMap(map.get("action"));
        MenuActionType type = MenuActionType.from(String.valueOf(actionMap.getOrDefault("type", "command")));
        String value = String.valueOf(actionMap.getOrDefault("value", ""));
        String itemPerm = String.valueOf(map.getOrDefault("permission", ""));
        if (itemPerm == null) itemPerm = "";

        Map<Integer, MenuAction> actions = menuActions.get(id);
        if (actions == null) {
            actions = new HashMap<>();
            menuActions.put(id, actions);
        }
        actions.put(slot, new MenuAction(type, value, itemPerm));
    }

}
