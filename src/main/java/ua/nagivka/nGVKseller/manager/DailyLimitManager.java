package ua.nagivka.nNGVKseller.manager;

import org.bukkit.entity.Player;
import ua.nagivka.nNGVKseller.NGVKseller;

import java.util.Calendar;

public class DailyLimitManager {
    private final NGVKseller plugin;

    public DailyLimitManager(NGVKseller plugin) {
        this.plugin = plugin;
        checkAndResetLimits();
    }

    public void checkAndResetLimits() {
        long lastReset = plugin.getDataManager().getLastResetTime();
        Calendar now = Calendar.getInstance();
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(lastReset);

        if (now.get(Calendar.DAY_OF_YEAR) != last.get(Calendar.DAY_OF_YEAR) ||
                now.get(Calendar.YEAR) != last.get(Calendar.YEAR)) {

            plugin.getDataManager().setLastResetTime(now.getTimeInMillis());
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
        if (!plugin.getConfigManager().getConfig().getBoolean("settings.daily-limits.enabled", true)) {
            return Double.MAX_VALUE;
        }
        checkAndResetLimits();
        double used = plugin.getDataManager().getPlayerTodayEarnings(player.getUniqueId());
        double max = getMaxLimit(player);
        return Math.max(0, max - used);
    }
}