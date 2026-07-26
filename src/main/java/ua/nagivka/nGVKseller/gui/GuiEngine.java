package ua.nagivka.nNGVKseller.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.nagivka.nNGVKseller.NGVKseller;
import ua.nagivka.nNGVKseller.config.ConfigManager;
import ua.nagivka.nNGVKseller.config.PageData;
import ua.nagivka.nNGVKseller.config.PriceData;
import ua.nagivka.nNGVKseller.manager.DynamicPriceManager;
import ua.nagivka.nNGVKseller.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public class GuiEngine {

    public static void openGui(Player player, int page) {
        ConfigManager cm = NGVKseller.getInstance().getConfigManager();
        PageData pageData = cm.getPages().get(page);

        if (pageData == null) return;

        Inventory inventory = Bukkit.createInventory(
                new SellerInventoryHolder(page),
                pageData.size(),
                ColorUtil.format(pageData.title())
        );

        pageData.items().forEach((slot, item) -> {
            ItemStack clone = item.clone();
            ItemMeta meta = clone.getItemMeta();

            if (meta != null) {
                var itemData = pageData.itemDataMap().get(slot);
                List<String> rawLore = cm.getGuiConfig().getStringList("pages." + page + ".items." + slot + ".lore");

                if (!rawLore.isEmpty()) {
                    List<Component> formattedLore = new ArrayList<>();

                    Material targetMat = itemData != null ? itemData.targetMaterial() : null;
                    PriceData priceData = targetMat != null ? cm.getPriceData(targetMat) : null;

                    int countAll = targetMat != null ? countPlayerItems(player, targetMat, priceData) : 0;
                    DynamicPriceManager priceMgr = NGVKseller.getInstance().getDynamicPriceManager();

                    double unitPrice = targetMat != null ? priceMgr.calculateSale(player, targetMat, 1, false).totalEarnings() : 0.0;
                    double stackPrice = targetMat != null ? priceMgr.calculateSale(player, targetMat, 64, false).totalEarnings() : 0.0;
                    double totalAllPrice = targetMat != null && countAll > 0 ? priceMgr.calculateSale(player, targetMat, countAll, false).totalEarnings() : 0.0;
                    String resetTimeStr = priceMgr.getFormattedResetTime();

                    for (String line : rawLore) {
                        if (targetMat != null) {
                            int rate = priceMgr.getRatePercent(player.getUniqueId(), targetMat);
                            line = line.replace("%rate%", String.valueOf(rate))
                                    .replace("%price_one%", String.format("%.2f", unitPrice))
                                    .replace("%price_stack%", String.format("%.2f", stackPrice))
                                    .replace("%price_all%", String.format("%.2f", totalAllPrice))
                                    .replace("%amount_all%", String.valueOf(countAll))
                                    .replace("%reset_time%", resetTimeStr);
                        }

                        double today = NGVKseller.getInstance().getDataManager().getPlayerTodayEarnings(player.getUniqueId());
                        double max = NGVKseller.getInstance().getDailyLimitManager().getMaxLimit(player);
                        double total = NGVKseller.getInstance().getDataManager().getPlayerTotalEarnings(player.getUniqueId());

                        line = line.replace("%today%", String.format("%.2f", today))
                                .replace("%max%", String.format("%.2f", max))
                                .replace("%total%", String.format("%.2f", total));

                        formattedLore.add(ColorUtil.format(line));
                    }
                    meta.lore(formattedLore);
                    clone.setItemMeta(meta);
                }
            }
            inventory.setItem(slot, clone);
        });

        player.openInventory(inventory);
    }

    private static int countPlayerItems(Player player, Material mat, PriceData priceData) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == mat) {
                if (priceData != null && priceData.customModelData() > 0) {
                    if (stack.hasItemMeta() && stack.getItemMeta().hasCustomModelData()
                            && stack.getItemMeta().getCustomModelData() == priceData.customModelData()) {
                        count += stack.getAmount();
                    }
                } else if (!stack.hasItemMeta() || (!stack.getItemMeta().hasEnchants() && !stack.getItemMeta().hasDisplayName())) {
                    count += stack.getAmount();
                }
            }
        }
        return count;
    }
}