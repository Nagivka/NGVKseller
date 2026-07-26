package ua.nagivka.nNGVKseller.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.nagivka.nNGVKseller.NGVKseller;
import ua.nagivka.nNGVKseller.util.ColorUtil;

import java.io.File;
import java.util.*;

public class ConfigManager {
    private final NGVKseller plugin;
    private FileConfiguration config;
    private FileConfiguration guiConfig;
    private FileConfiguration pricesConfig;

    private final Map<Integer, PageData> pages = new HashMap<>();
    private final Map<Material, PriceData> prices = new HashMap<>();

    public ConfigManager(NGVKseller plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        pages.clear();
        prices.clear();

        config = loadOrCreate("config.yml");
        guiConfig = loadOrCreate("gui.yml");
        pricesConfig = loadOrCreate("prices.yml");

        loadPrices();
        loadPages();
    }

    private void loadPrices() {
        ConfigurationSection priceSec = pricesConfig.getConfigurationSection("prices");
        if (priceSec == null) return;

        for (String key : priceSec.getKeys(false)) {
            Material mat = Material.matchMaterial(key.toUpperCase());
            if (mat == null) continue;

            if (priceSec.isConfigurationSection(key)) {
                ConfigurationSection itemSec = priceSec.getConfigurationSection(key);
                double base = itemSec.getDouble("price", 10.0);
                double min = itemSec.getDouble("min-price", base * 0.3);
                double max = itemSec.getDouble("max-price", base * 2.0);
                int cmd = itemSec.getInt("custom-model-data", 0);
                prices.put(mat, new PriceData(mat, base, min, max, cmd));
            } else {
                double base = priceSec.getDouble(key, 10.0);
                prices.put(mat, new PriceData(mat, base, base * 0.3, base * 2.0, 0));
            }
        }
    }

    private void loadPages() {
        ConfigurationSection pagesSec = guiConfig.getConfigurationSection("pages");
        if (pagesSec == null) return;

        for (String pageKey : pagesSec.getKeys(false)) {
            try {
                int pageNum = Integer.parseInt(pageKey);
                ConfigurationSection pageSec = pagesSec.getConfigurationSection(pageKey);
                if (pageSec == null) continue;

                String title = pageSec.getString("title", "Скупщик");
                int size = pageSec.getInt("size", 36);
                if (size % 9 != 0 || size < 9 || size > 54) size = 36;

                Map<Integer, ItemStack> items = new HashMap<>();
                Map<Integer, GuiItemData> itemDataMap = new HashMap<>();

                ConfigurationSection itemsSec = pageSec.getConfigurationSection("items");
                if (itemsSec != null) {
                    for (String slotKey : itemsSec.getKeys(false)) {
                        int slot = Integer.parseInt(slotKey);
                        ConfigurationSection itemSec = itemsSec.getConfigurationSection(slotKey);
                        if (itemSec == null) continue;

                        String matName = itemSec.getString("material", "STONE").toUpperCase();
                        Material mat = Material.matchMaterial(matName);
                        if (mat == null) mat = Material.STONE;

                        String name = itemSec.getString("name");
                        String action = itemSec.getString("action", "NONE").toUpperCase();
                        String itemType = itemSec.getString("item-type");

                        ItemStack itemStack = new ItemStack(mat);
                        ItemMeta meta = itemStack.getItemMeta();
                        if (meta != null) {
                            if (name != null) meta.displayName(ColorUtil.format(name));
                            if (itemSec.contains("custom-model-data")) {
                                meta.setCustomModelData(itemSec.getInt("custom-model-data"));
                            }
                            itemStack.setItemMeta(meta);
                        }

                        Material targetMat = null;
                        if (itemType != null) {
                            targetMat = Material.matchMaterial(itemType.toUpperCase());
                        }

                        items.put(slot, itemStack);
                        itemDataMap.put(slot, new GuiItemData(action, targetMat));
                    }
                }

                pages.put(pageNum, new PageData(title, size, items, itemDataMap));
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading page " + pageKey + ": " + e.getMessage());
            }
        }
    }

    private FileConfiguration loadOrCreate(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getGuiConfig() { return guiConfig; }
    public FileConfiguration getPricesConfig() { return pricesConfig; }
    public String getMessage(String path) { return config.getString("messages." + path, ""); }
    public Map<Integer, PageData> getPages() { return pages; }
    public Map<Material, PriceData> getPrices() { return prices; }
    public PriceData getPriceData(Material material) { return prices.get(material); }
}