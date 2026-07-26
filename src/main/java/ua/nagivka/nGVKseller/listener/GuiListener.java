package ua.nagivka.nNGVKseller.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ua.nagivka.nNGVKseller.NGVKseller;
import ua.nagivka.nNGVKseller.config.ConfigManager;
import ua.nagivka.nNGVKseller.config.GuiItemData;
import ua.nagivka.nNGVKseller.config.PageData;
import ua.nagivka.nNGVKseller.gui.GuiEngine;
import ua.nagivka.nNGVKseller.gui.SellerInventoryHolder;
import ua.nagivka.nNGVKseller.manager.DataManager;
import ua.nagivka.nNGVKseller.util.ColorUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuiListener implements Listener {
    private final NGVKseller plugin;
    private final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();

    public GuiListener(NGVKseller plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clickCooldowns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellerInventoryHolder holder)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) return;

        long now = System.currentTimeMillis();
        if (clickCooldowns.getOrDefault(player.getUniqueId(), 0L) > now) return;
        clickCooldowns.put(player.getUniqueId(), now + 200L);

        int slot = event.getSlot();
        int currentPage = holder.getPage();

        ConfigManager cm = plugin.getConfigManager();
        PageData pageData = cm.getPages().get(currentPage);
        if (pageData == null) return;

        GuiItemData itemData = pageData.itemDataMap().get(slot);
        if (itemData == null) return;

        switch (itemData.action()) {
            case "NEXT_PAGE" -> {
                if (cm.getPages().containsKey(currentPage + 1)) {
                    GuiEngine.openGui(player, currentPage + 1);
                }
            }
            case "PREV_PAGE" -> {
                if (currentPage > 1 && cm.getPages().containsKey(currentPage - 1)) {
                    GuiEngine.openGui(player, currentPage - 1);
                }
            }
            case "SELL" -> {
                if (itemData.targetMaterial() != null) {
                    if (event.isShiftClick() && event.isRightClick()) {
                        plugin.getSellManager().processSell(player, itemData.targetMaterial(), Integer.MAX_VALUE, true);
                    } else if (event.isRightClick()) {
                        plugin.getSellManager().processSell(player, itemData.targetMaterial(), 64, true);
                    } else if (event.isLeftClick()) {
                        plugin.getSellManager().processSell(player, itemData.targetMaterial(), 1, true);
                    }
                    GuiEngine.openGui(player, currentPage);
                }
            }
            case "SELL_ALL" -> {
                plugin.getSellManager().sellAll(player);
                GuiEngine.openGui(player, currentPage);
            }
            case "STATS" -> {
                double today = plugin.getDataManager().getPlayerTodayEarnings(player.getUniqueId());
                double max = plugin.getDailyLimitManager().getMaxLimit(player);
                double total = plugin.getDataManager().getPlayerTotalEarnings(player.getUniqueId());

                String msg = cm.getMessage("stats-info")
                        .replace("%player%", player.getName())
                        .replace("%today%", String.format("%.2f", today))
                        .replace("%max%", String.format("%.2f", max))
                        .replace("%total%", String.format("%.2f", total));
                player.sendMessage(ColorUtil.format(msg));
            }
            case "TOP" -> {
                List<DataManager.TopEntry> top = plugin.getDataManager().getTopPlayers(10);
                player.sendMessage(ColorUtil.format("#E8D8C8--- ТОП-10 ТОРГОВЦЕВ СЕРВЕРА ---"));
                for (int i = 0; i < top.size(); i++) {
                    DataManager.TopEntry entry = top.get(i);
                    String line = String.format("#9AA8B6%d. #F4E2D8%s #9AA8B6— #B4E197&fƜ%.2f", (i + 1), entry.name(), entry.total());
                    player.sendMessage(ColorUtil.format(line));
                }
            }
        }
    }
}