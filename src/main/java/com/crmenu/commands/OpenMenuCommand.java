package com.crmenu.commands;

import com.crmenu.CrMenuPlugin;
import com.crmenu.menu.MenuManager;
import com.crmenu.util.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpenMenuCommand implements CommandExecutor {

    private final CrMenuPlugin plugin;
    private final MenuManager menuManager;
    private final Language language;

    public OpenMenuCommand(CrMenuPlugin plugin, MenuManager menuManager, Language language) {
        this.plugin = plugin;
        this.menuManager = menuManager;
        this.language = language;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Leer permisos configurables
        String permOpenCmd = plugin.getConfig().getString("permissions.command_open", "crmenu.open");
        String permAdminReload = plugin.getConfig().getString("permissions.admin_reload", "crmenu.reload");
        // Subcomando info: disponible para todos
        if (args.length >= 1 && args[0].equalsIgnoreCase("info")) {
            sendInfo(sender);
            return true;
        }

        // Subcomando reload: requiere permiso crmenu.reload
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(permAdminReload)) {
                sender.sendMessage(language.color(language.get("no_permission")));
                return true;
            }
            plugin.reloadPlugin();
            if (sender instanceof Player) {
                sender.sendMessage(language.color("&aCrMenu recargado."));
            } else {
                // Consola: incluir prefijo
                sender.sendMessage(language.color(language.get("prefix") + " &aCrMenu recargado."));
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission(permOpenCmd)) {
            player.sendMessage(language.color(language.get("no_permission")));
            return true;
        }

        player.sendMessage(language.color(language.get("open_message")));
        if (args.length >= 1) {
            String id = args[0];
            menuManager.openMenu(player, id);
        } else {
            menuManager.openMenu(player);
        }
        return true;
    }

    private void sendInfo(CommandSender sender) {
        String name = plugin.getDescription().getName();
        String version = plugin.getDescription().getVersion();
        String header = (sender instanceof Player)
                ? "&6" + name + " &7v" + version
                : language.get("prefix") + " &6" + name + " &7v" + version;
        sender.sendMessage(language.color(header));
        sender.sendMessage(language.color("&7Comandos disponibles:"));
        sender.sendMessage(language.color("&e/menu &7- Abre el menú principal"));
        sender.sendMessage(language.color("&e/menu <id> &7- Abre un submenú por ID"));
        sender.sendMessage(language.color("&e/menu info &7- Muestra información del plugin"));
        sender.sendMessage(language.color("&e/menu reload &7- Recarga configuración (requiere permiso)"));
        sender.sendMessage(language.color("&e/crmenu reload &7- Recarga configuración"));
        sender.sendMessage(language.color("&e/crmenu diag &7- Diagnóstico de configuración"));
        sender.sendMessage(language.color("&7Acciones de ítems: &fcommand, command_console, message, open_menu"));
    }
}