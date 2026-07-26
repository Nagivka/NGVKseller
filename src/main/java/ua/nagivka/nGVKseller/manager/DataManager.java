package ua.nagivka.nNGVKseller.manager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.nagivka.nNGVKseller.NGVKseller;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private final NGVKseller plugin;
    private File file;
    private FileConfiguration dataConfig;

    private final Map<UUID, Double> todayEarnings = new ConcurrentHashMap<>();
    private final Map<UUID, Double> totalEarnings = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerNames = new ConcurrentHashMap<>();

    public record TopEntry(String name, double total) {}

    public DataManager(NGVKseller plugin) {
        this.plugin = plugin;
    }

    public void loadData() {
        file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("File creation error: data.yml");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection sec = dataConfig.getConfigurationSection("players");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    todayEarnings.put(uuid, sec.getDouble(key + ".today", 0.0));
                    totalEarnings.put(uuid, sec.getDouble(key + ".total", 0.0));
                    if (sec.contains(key + ".name")) {
                        playerNames.put(uuid, sec.getString(key + ".name"));
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public synchronized void saveData() {
        if (dataConfig == null || file == null) return;

        todayEarnings.forEach((uuid, value) -> dataConfig.set("players." + uuid + ".today", value));
        totalEarnings.forEach((uuid, value) -> dataConfig.set("players." + uuid + ".total", value));
        playerNames.forEach((uuid, value) -> dataConfig.set("players." + uuid + ".name", value));

        try {
            dataConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Save error: data.yml");
        }
    }

    public double getPlayerTodayEarnings(UUID uuid) {
        return todayEarnings.getOrDefault(uuid, 0.0);
    }

    public double getPlayerTotalEarnings(UUID uuid) {
        return totalEarnings.getOrDefault(uuid, 0.0);
    }

    public void addEarnings(UUID uuid, String name, double amount) {
        todayEarnings.merge(uuid, amount, Double::sum);
        totalEarnings.merge(uuid, amount, Double::sum);
        playerNames.put(uuid, name);
    }

    public void resetPlayerLimit(UUID uuid) {
        todayEarnings.put(uuid, 0.0);
        if (dataConfig != null) {
            dataConfig.set("players." + uuid + ".today", 0.0);
        }
    }

    public long getLastResetTime() {
        return dataConfig.getLong("last-reset-time", 0L);
    }

    public void setLastResetTime(long time) {
        dataConfig.set("last-reset-time", time);
    }

    public Map<Material, Double> getPlayerMultipliers(UUID uuid) {
        Map<Material, Double> map = new HashMap<>();
        ConfigurationSection sec = dataConfig.getConfigurationSection("players." + uuid + ".prices");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat != null) {
                    map.put(mat, sec.getDouble(key, 1.0));
                }
            }
        }
        return map;
    }

    public void savePlayerMultipliers(UUID uuid, Map<Material, Double> multipliers) {
        multipliers.forEach((mat, mult) -> dataConfig.set("players." + uuid + ".prices." + mat.name(), mult));
    }

    public void clearAllPlayerPrices() {
        ConfigurationSection sec = dataConfig.getConfigurationSection("players");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                dataConfig.set("players." + key + ".prices", null);
            }
        }
    }

    public List<TopEntry> getTopPlayers(int limit) {
        List<TopEntry> list = new ArrayList<>();
        totalEarnings.forEach((uuid, total) -> {
            if (total > 0) {
                String name = playerNames.getOrDefault(uuid, "Unknown");
                list.add(new TopEntry(name, total));
            }
        });
        list.sort((a, b) -> Double.compare(b.total(), a.total()));
        return list.stream().limit(limit).toList();
    }
}