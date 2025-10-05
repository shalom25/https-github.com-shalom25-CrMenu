package com.crmenu.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class Language {

    private final Plugin plugin;
    private final String code;
    private YamlConfiguration lang;

    public Language(Plugin plugin, String code) {
        this.plugin = plugin;
        this.code = code;
        load();
    }

    private void load() {
        File dir = new File(plugin.getDataFolder(), "lang");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, code + ".yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("lang/" + code + ".yml", false);
            } catch (IllegalArgumentException ignored) {
                // Si el recurso no existe en el JAR, crear archivo con contenidos por defecto
                try {
                    if ("es".equalsIgnoreCase(code)) {
                        org.bukkit.configuration.file.YamlConfiguration y = new org.bukkit.configuration.file.YamlConfiguration();
                        y.set("prefix", "CrMenu");
                        y.set("menu_title", "&8Menú del Servidor");
                        y.set("open_message", "&aAbriendo menú...");
                        y.set("no_permission", "&cNo tienes permiso para usar esto.");
                        y.save(file);
                    } else {
                        // Por defecto inglés
                        org.bukkit.configuration.file.YamlConfiguration y = new org.bukkit.configuration.file.YamlConfiguration();
                        y.set("prefix", "CrMenu");
                        y.set("menu_title", "&8Server Menu");
                        y.set("open_message", "&aOpening menu...");
                        y.set("no_permission", "&cYou don't have permission to use this.");
                        y.save(file);
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("No se pudo crear el archivo de idioma '" + code + ".yml': " + e.getMessage());
                    // Fallback a inglés en memoria sin archivo
                    file = new File(dir, "en.yml");
                }
            }
        }
        lang = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key) {
        String val = lang.getString(key, key);
        return val;
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}