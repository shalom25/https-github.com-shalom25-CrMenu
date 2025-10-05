package com.crmenu;

import com.crmenu.commands.OpenMenuCommand;
import com.crmenu.commands.ReloadCommand;
import com.crmenu.menu.MenuManager;
import com.crmenu.util.Language;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class CrMenuPlugin extends JavaPlugin {

    private MenuManager menuManager;
    private Language language;
    private Listener clickListener;

    @Override
    public void onEnable() {
        Logger log = getLogger();

        // Crear config por defecto
        saveDefaultConfig();

        // Guardar recursos de idioma si existen en el JAR, sin fallar si faltan
        try {
            saveResource("lang/en.yml", false);
        } catch (IllegalArgumentException e) {
            log.warning("Recurso embebido faltante: lang/en.yml (se generará en runtime si es necesario)");
        }
        try {
            saveResource("lang/es.yml", false);
        } catch (IllegalArgumentException e) {
            log.warning("Recurso embebido faltante: lang/es.yml (se generará en runtime si es necesario)");
        }

        // Inicializar idioma y menú
        String code = getConfig().getString("language", "en");
        language = new Language(this, code);
        menuManager = new MenuManager(this, language);

        // Registrar listener de clics
        clickListener = menuManager.getClickListener();
        getServer().getPluginManager().registerEvents(clickListener, this);

        // Registrar comandos
        PluginCommand menuCmd = getCommand("menu");
        if (menuCmd != null) {
            menuCmd.setExecutor(new OpenMenuCommand(this, menuManager, language));
        } else {
            log.warning("Comando 'menu' no encontrado en plugin.yml");
        }

        PluginCommand adminCmd = getCommand("crmenu");
        if (adminCmd != null) {
            adminCmd.setExecutor(new ReloadCommand(this));
        } else {
            log.warning("Comando 'crmenu' no encontrado en plugin.yml");
        }

        log.info("CrMenu habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        // Desregistrar listener
        if (clickListener != null) {
            HandlerList.unregisterAll(clickListener);
        }
    }

    public void reloadPlugin() {
        // Recargar configuración y reconstruir estado
        reloadConfig();
        String code = getConfig().getString("language", "en");
        language = new Language(this, code);

        // Reemplazar gestor de menú
        menuManager = new MenuManager(this, language);

        // Re-registrar listener (el anterior se desregistra)
        if (clickListener != null) {
            HandlerList.unregisterAll(clickListener);
        }
        clickListener = menuManager.getClickListener();
        getServer().getPluginManager().registerEvents(clickListener, this);
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }
}