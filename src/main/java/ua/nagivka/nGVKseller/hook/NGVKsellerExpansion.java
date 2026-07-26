package ua.nagivka.nNGVKseller.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.nagivka.nNGVKseller.NGVKseller;
import ua.nagivka.nNGVKseller.manager.DataManager;

import java.util.List;

public class NGVKsellerExpansion extends PlaceholderExpansion {
    private final NGVKseller plugin;

    public NGVKsellerExpansion(NGVKseller plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "ngvkseller"; }

    @Override
    public @NotNull String getAuthor() { return "NGVK"; }

    @Override
    public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player != null) {
            if (params.equalsIgnoreCase("today")) {
                return String.format("%.2f", plugin.getDataManager().getPlayerTodayEarnings(player.getUniqueId()));
            }
            if (params.equalsIgnoreCase("total")) {
                return String.format("%.2f", plugin.getDataManager().getPlayerTotalEarnings(player.getUniqueId()));
            }
            if (params.equalsIgnoreCase("limit_max")) {
                return String.format("%.2f", plugin.getDailyLimitManager().getMaxLimit(player));
            }
            if (params.equalsIgnoreCase("limit_remaining")) {
                return String.format("%.2f", plugin.getDailyLimitManager().getRemainingLimit(player));
            }
            if (params.startsWith("rate_")) {
                String matStr = params.substring(5).toUpperCase();
                Material mat = Material.matchMaterial(matStr);
                if (mat != null) {
                    return String.valueOf(plugin.getDynamicPriceManager().getRatePercent(player.getUniqueId(), mat));
                }
            }
        }

        if (params.equalsIgnoreCase("reset_time")) {
            return plugin.getDynamicPriceManager().getFormattedResetTime();
        }

        if (params.startsWith("top_name_")) {
            int index = parseIndex(params.substring(9)) - 1;
            List<DataManager.TopEntry> top = plugin.getDataManager().getTopPlayers(10);
            return (index >= 0 && index < top.size()) ? top.get(index).name() : "—";
        }

        if (params.startsWith("top_amount_")) {
            int index = parseIndex(params.substring(11)) - 1;
            List<DataManager.TopEntry> top = plugin.getDataManager().getTopPlayers(10);
            return (index >= 0 && index < top.size()) ? String.format("%.2f", top.get(index).total()) : "0.00";
        }

        return null;
    }

    private int parseIndex(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}