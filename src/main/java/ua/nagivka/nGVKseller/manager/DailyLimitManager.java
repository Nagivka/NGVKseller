package ua.nagivka.nNGVKseller.manager;

import org.bukkit.entity.Player;
import ua.nagivka.nNGVKseller.NGVKseller;

import java.time.LocalDate;

public class DailyLimitManager {
    private final NGVKseller plugin;

    public DailyLimitManager(NGVKseller plugin) {
        this.plugin = plugin;
        checkAndResetLimits();
    }

    public synchronized void checkAndResetLimits() {
        String lastResetDate = plugin.getDataManager().getLastResetDate();
        String currentDate = LocalDate.now().toString();

        if (!currentDate.equals(lastResetDate)) {
            plugin.getDataManager().resetAllTodayEarnings();
            plugin.getDataManager().setLastResetDate(currentDate);
            plugin.getDataManager().saveData();
        }
    }

    public double getMaxLimit(Player player) {
        double baseLimit = plugin.getConfigManager().getConfig().getDouble("settings.daily-limits.default-max-amount", 50000.0);
        double multiplier = 1.0;

        var section = plugin.getConfigManager().getConfig().getConfigurationSection("settings.multipliers");
        if (section != null) {
            for (String perm : section.getKeys(false)) {
                if (player.hasPermission(perm)) {
                    double mult = section.getDouble(perm, 1.0);
                    if (mult > multiplier) multiplier = mult;
                }
            }
        }
        return baseLimit * multiplier;
    }

    public double getRemainingLimit(Player player) {
        checkAndResetLimits();
        if (!plugin.getConfigManager().getConfig().getBoolean("settings.daily-limits.enabled", true)) {
            return Double.MAX_VALUE;
        }
        double used = plugin.getDataManager().getPlayerTodayEarnings(player.getUniqueId());
        double max = getMaxLimit(player);
        return Math.max(0, max - used);
    }
}