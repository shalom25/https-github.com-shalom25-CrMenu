package com.crmenu.commands;

import com.crmenu.CrMenuPlugin;
import com.crmenu.menu.MenuManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ReloadCommand implements CommandExecutor {

    private final CrMenuPlugin plugin;

    public ReloadCommand(CrMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String permAdminDiag = plugin.getConfig().getString("permissions.admin_diag", "crmenu.diag");
        String permAdminReload = plugin.getConfig().getString("permissions.admin_reload", "crmenu.reload");
        if (args.length > 0 && args[0].equalsIgnoreCase("diag")) {
            if (!sender.hasPermission(permAdminDiag)) {
                sender.sendMessage("§cNo tienes permiso para ejecutar diagnósticos.");
                return true;
            }
            MenuManager manager = plugin.getMenuManager();
            List<String> report = manager.diagnoseConfig();
            sender.sendMessage("§6CrMenu Diagnóstico:");
            for (String line : report) {
                sender.sendMessage("§7" + line);
            }
            return true;
        }

        // Subcomando: regen (regenera config.yml desde el JAR y recarga el plugin)
        if (args.length > 0 && args[0].equalsIgnoreCase("regen")) {
            if (!sender.hasPermission(permAdminReload)) {
                sender.sendMessage("§cNo tienes permiso para regenerar configuración.");
                return true;
            }
            try {
                // Sobrescribe el config.yml con el recurso embebido
                plugin.saveResource("config.yml", true);
                sender.sendMessage("§aConfig.yml regenerado desde el JAR.");
            } catch (Exception e) {
                sender.sendMessage("§cError al regenerar config.yml: " + e.getMessage());
                return true;
            }
            // Recargar para aplicar cambios
            plugin.reloadPlugin();
            sender.sendMessage("§aCrMenu recargado.");
            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.playSound(p.getLocation(), Sound.UI_TOAST_IN, 1f, 1f);
            }
            return true;
        }

        if (!sender.hasPermission(permAdminReload)) {
            sender.sendMessage("§cNo tienes permiso para recargar.");
            return true;
        }

        plugin.reloadPlugin();
        sender.sendMessage("§aCrMenu recargado.");
        if (sender instanceof Player) {
            Player p = (Player) sender;
            p.playSound(p.getLocation(), Sound.UI_TOAST_IN, 1f, 1f);
        }
        return true;
    }
}