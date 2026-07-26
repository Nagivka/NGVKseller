package ua.nagivka.nNGVKseller.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.nagivka.nNGVKseller.NGVKseller;
import ua.nagivka.nNGVKseller.gui.GuiEngine;
import ua.nagivka.nNGVKseller.manager.DataManager;
import ua.nagivka.nNGVKseller.util.ColorUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SellerCommand implements CommandExecutor, TabCompleter {
    private final NGVKseller plugin;

    public SellerCommand(NGVKseller plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            String sub = args[0].toLowerCase();

            switch (sub) {
                case "reload" -> {
                    if (!sender.hasPermission("ngvkseller.admin")) {
                        sendMessage(sender, "no-permission");
                        return true;
                    }
                    plugin.getConfigManager().loadConfigs();
                    sendMessage(sender, "reload");
                    return true;
                }
                case "resetlimit" -> {
                    if (!sender.hasPermission("ngvkseller.admin")) {
                        sendMessage(sender, "no-permission");
                        return true;
                    }
                    if (args.length < 2) {
                        sender.sendMessage("Использование: /seller resetlimit <игрок>");
                        return true;
                    }
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                    plugin.getDataManager().resetPlayerLimit(target.getUniqueId());
                    String msg = plugin.getConfigManager().getMessage("limit-reset-player")
                            .replace("%player%", target.getName() != null ? target.getName() : args[1]);
                    sender.sendMessage(ColorUtil.format(msg));
                    return true;
                }
                case "top" -> {
                    List<DataManager.TopEntry> top = plugin.getDataManager().getTopPlayers(10);
                    sender.sendMessage(ColorUtil.format("#E8D8C8--- ТОП-10 ТОРГОВЦЕВ СЕРВЕРА ---"));
                    for (int i = 0; i < top.size(); i++) {
                        DataManager.TopEntry entry = top.get(i);
                        String line = String.format("#9AA8B6%d. #F4E2D8%s #9AA8B6— #B4E197$%.2f", (i + 1), entry.name(), entry.total());
                        sender.sendMessage(ColorUtil.format(line));
                    }
                    return true;
                }
                case "sellhand", "hand" -> {
                    if (sender instanceof Player p) {
                        plugin.getSellManager().sellHand(p);
                    }
                    return true;
                }
                case "sellall", "all" -> {
                    if (sender instanceof Player p) {
                        plugin.getSellManager().sellAll(p);
                    }
                    return true;
                }
            }
        }

        if (!(sender instanceof Player player)) {
            sendMessage(sender, "only-players");
            return true;
        }

        if (!player.hasPermission("ngvkseller.use")) {
            sendMessage(player, "no-permission");
            return true;
        }

        GuiEngine.openGui(player, 1);
        return true;
    }

    private void sendMessage(CommandSender sender, String messageKey) {
        String msg = plugin.getConfigManager().getMessage(messageKey);
        if (!msg.isEmpty()) sender.sendMessage(ColorUtil.format(msg));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("hand", "all", "top"));
            if (sender.hasPermission("ngvkseller.admin")) {
                completions.add("reload");
                completions.add("resetlimit");
            }
            return completions.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}