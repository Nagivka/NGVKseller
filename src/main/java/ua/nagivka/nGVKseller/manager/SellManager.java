package ua.nagivka.nNGVKseller.manager;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.nagivka.nNGVKseller.NGVKseller;
import ua.nagivka.nNGVKseller.config.PriceData;
import ua.nagivka.nNGVKseller.util.ColorUtil;

public class SellManager {
    private final NGVKseller plugin;

    public SellManager(NGVKseller plugin) {
        this.plugin = plugin;
    }

    public void sellHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            sendMessage(player, "nothing-to-sell");
            playSound(player, "sell-failure");
            return;
        }

        PriceData priceData = plugin.getConfigManager().getPriceData(item.getType());
        if (priceData == null || !matchesData(item, priceData)) {
            sendMessage(player, "nothing-to-sell");
            playSound(player, "sell-failure");
            return;
        }

        processSell(player, item.getType(), item.getAmount(), true);
    }

    public void sellAll(Player player) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        int totalSold = 0;
        double totalEarnings = 0.0;

        for (int i = 0; i < storage.length; i++) {
            ItemStack stack = storage[i];
            if (stack == null || stack.getType().isAir()) continue;

            PriceData data = plugin.getConfigManager().getPriceData(stack.getType());
            if (data != null && matchesData(stack, data)) {
                DynamicPriceManager.SaleResult result = plugin.getDynamicPriceManager().calculateSale(player, stack.getType(), stack.getAmount(), true);

                if (result.actualSoldAmount() > 0) {
                    totalSold += result.actualSoldAmount();
                    totalEarnings += result.totalEarnings();

                    if (result.actualSoldAmount() == stack.getAmount()) {
                        storage[i] = null;
                    } else {
                        stack.setAmount(stack.getAmount() - result.actualSoldAmount());
                    }
                }
            }
        }

        if (totalSold > 0) {
            player.getInventory().setStorageContents(storage);
            executeTransaction(player, "ALL", totalSold, totalEarnings);
        } else {
            sendMessage(player, "nothing-to-sell");
            playSound(player, "sell-failure");
        }
    }

    public void processSell(Player player, Material mat, int requestedAmount, boolean singleType) {
        PriceData priceData = plugin.getConfigManager().getPriceData(mat);
        if (priceData == null) return;

        ItemStack[] storage = player.getInventory().getStorageContents();
        int availableAmount = 0;

        for (ItemStack stack : storage) {
            if (stack != null && stack.getType() == mat && matchesData(stack, priceData)) {
                availableAmount += stack.getAmount();
            }
        }

        if (availableAmount == 0) {
            sendMessage(player, "nothing-to-sell");
            playSound(player, "sell-failure");
            return;
        }

        int amountToProcess = Math.min(availableAmount, requestedAmount);
        DynamicPriceManager.SaleResult result = plugin.getDynamicPriceManager().calculateSale(player, mat, amountToProcess, true);

        if (result.actualSoldAmount() <= 0) {
            sendMessage(player, "limit-reached");
            playSound(player, "sell-failure");
            return;
        }

        int leftToDeduct = result.actualSoldAmount();
        for (int i = 0; i < storage.length; i++) {
            ItemStack stack = storage[i];
            if (stack != null && stack.getType() == mat && matchesData(stack, priceData)) {
                if (stack.getAmount() <= leftToDeduct) {
                    leftToDeduct -= stack.getAmount();
                    storage[i] = null;
                } else {
                    stack.setAmount(stack.getAmount() - leftToDeduct);
                    leftToDeduct = 0;
                }
            }
            if (leftToDeduct <= 0) break;
        }

        player.getInventory().setStorageContents(storage);
        executeTransaction(player, mat.name(), result.actualSoldAmount(), result.totalEarnings());
    }

    private void executeTransaction(Player player, String itemName, int amount, double earnings) {
        plugin.getEconomy().depositPlayer(player, earnings);
        plugin.getDataManager().addEarnings(player.getUniqueId(), player.getName(), earnings);

        playSound(player, "sell-success");

        String msg = plugin.getConfigManager().getMessage("sold-items")
                .replace("%amount%", String.valueOf(amount))
                .replace("%item%", itemName)
                .replace("%price%", String.format("%.2f", earnings));
        player.sendMessage(ColorUtil.format(msg));
    }

    private boolean matchesData(ItemStack stack, PriceData data) {
        if (data.customModelData() > 0) {
            if (!stack.hasItemMeta()) return false;
            ItemMeta meta = stack.getItemMeta();
            return meta.hasCustomModelData() && meta.getCustomModelData() == data.customModelData();
        }

        if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta.hasEnchants()) return false;
            if (meta.hasDisplayName()) return false;
        }
        return true;
    }

    private void sendMessage(Player player, String key) {
        String msg = plugin.getConfigManager().getMessage(key);
        if (!msg.isEmpty()) player.sendMessage(ColorUtil.format(msg));
    }

    private void playSound(Player player, String soundKey) {
        if (!plugin.getConfigManager().getConfig().getBoolean("settings.sounds.enabled", true)) return;
        String soundName = plugin.getConfigManager().getConfig().getString("settings.sounds." + soundKey, "");
        if (!soundName.isEmpty()) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
            } catch (Exception ignored) {}
        }
    }
}