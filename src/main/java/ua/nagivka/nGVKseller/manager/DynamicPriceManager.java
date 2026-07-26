package ua.nagivka.nNGVKseller.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ua.nagivka.nNGVKseller.NGVKseller;
import ua.nagivka.nNGVKseller.config.PriceData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicPriceManager {
    private final NGVKseller plugin;
    private final Map<UUID, Map<Material, Double>> playerMultipliers = new ConcurrentHashMap<>();

    public DynamicPriceManager(NGVKseller plugin) {
        this.plugin = plugin;
    }

    public void startRecoveryTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            int hours = plugin.getConfigManager().getConfig().getInt("settings.dynamic-prices.reset-interval-hours", 3);
            long period = hours * 3600 * 1000L;
            long now = System.currentTimeMillis();
            long nextReset = getNextResetTimestamp();

            if (now >= nextReset) {
                playerMultipliers.clear();
                plugin.getDataManager().clearAllPlayerPrices();
                plugin.getDataManager().setLastResetTime(now);
            }
        }, 20L, 20L);
    }

    public long getNextResetTimestamp() {
        int hours = plugin.getConfigManager().getConfig().getInt("settings.dynamic-prices.reset-interval-hours", 3);
        long period = hours * 3600 * 1000L;
        long lastReset = plugin.getDataManager().getLastResetTime();
        if (lastReset <= 0) {
            long now = System.currentTimeMillis();
            lastReset = now - (now % period);
            plugin.getDataManager().setLastResetTime(lastReset);
        }
        return lastReset + period;
    }

    public String getFormattedResetTime() {
        long remaining = getNextResetTimestamp() - System.currentTimeMillis();
        if (remaining <= 0) return "00:00";

        long minutes = (remaining / (1000 * 60)) % 60;
        long hours = (remaining / (1000 * 60 * 60));

        return String.format("%02d:%02d", hours, minutes);
    }

    public double getPlayerMultiplier(UUID uuid, Material mat) {
        Map<Material, Double> map = playerMultipliers.computeIfAbsent(uuid, k -> plugin.getDataManager().getPlayerMultipliers(uuid));
        return map.getOrDefault(mat, 1.0);
    }

    public int getRatePercent(UUID uuid, Material mat) {
        double mult = getPlayerMultiplier(uuid, mat);
        return (int) Math.round(mult * 100);
    }

    public record SaleResult(double totalEarnings, int actualSoldAmount, double finalMultiplier) {}

    public SaleResult calculateSale(Player player, Material mat, int requestedAmount, boolean apply) {
        PriceData data = plugin.getConfigManager().getPriceData(mat);
        if (data == null || requestedAmount <= 0) {
            return new SaleResult(0.0, 0, getPlayerMultiplier(player.getUniqueId(), mat));
        }

        UUID uuid = player.getUniqueId();
        double remainingDailyLimit = plugin.getDailyLimitManager().getRemainingLimit(player);
        if (remainingDailyLimit <= 0) {
            return new SaleResult(0.0, 0, getPlayerMultiplier(uuid, mat));
        }

        boolean dynamicEnabled = plugin.getConfigManager().getConfig().getBoolean("settings.dynamic-prices.enabled", true);
        double dropPerItem = dynamicEnabled ? plugin.getConfigManager().getConfig().getDouble("settings.dynamic-prices.drop-per-item", 0.0005) : 0.0;
        double currentMult = getPlayerMultiplier(uuid, mat);
        double minMult = data.basePrice() > 0 ? (data.minPrice() / data.basePrice()) : 0.1;

        double totalEarnings = 0.0;
        int soldCount = 0;

        for (int i = 0; i < requestedAmount; i++) {
            double itemPrice = dynamicEnabled
                    ? Math.max(data.minPrice(), Math.min(data.maxPrice(), data.basePrice() * currentMult))
                    : data.basePrice();

            if (totalEarnings + itemPrice > remainingDailyLimit) {
                break;
            }

            totalEarnings += itemPrice;
            soldCount++;

            if (dynamicEnabled) {
                currentMult = Math.max(minMult, currentMult - dropPerItem);
            }
        }

        if (apply && soldCount > 0 && dynamicEnabled) {
            Map<Material, Double> map = playerMultipliers.computeIfAbsent(uuid, k -> plugin.getDataManager().getPlayerMultipliers(uuid));
            map.put(mat, currentMult);
            plugin.getDataManager().savePlayerMultipliers(uuid, map);
        }

        return new SaleResult(totalEarnings, soldCount, currentMult);
    }
}