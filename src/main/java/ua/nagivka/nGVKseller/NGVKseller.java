package ua.nagivka.nNGVKseller;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ua.nagivka.nNGVKseller.command.SellerCommand;
import ua.nagivka.nNGVKseller.config.ConfigManager;
import ua.nagivka.nNGVKseller.hook.NGVKsellerExpansion;
import ua.nagivka.nNGVKseller.listener.GuiListener;
import ua.nagivka.nNGVKseller.manager.*;

public class NGVKseller extends JavaPlugin {
    private static NGVKseller instance;
    private Economy econ = null;

    private ConfigManager configManager;
    private DataManager dataManager;
    private DailyLimitManager dailyLimitManager;
    private DynamicPriceManager dynamicPriceManager;
    private SellManager sellManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            getLogger().severe("Vault error!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigs();

        this.dataManager = new DataManager(this);
        this.dataManager.loadData();

        this.dailyLimitManager = new DailyLimitManager(this);
        this.dynamicPriceManager = new DynamicPriceManager(this);
        this.sellManager = new SellManager(this);

        this.dynamicPriceManager.startRecoveryTask();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (dataManager != null) {
                dataManager.saveData();
            }
        }, 6000L, 6000L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (dailyLimitManager != null) {
                dailyLimitManager.checkAndResetLimits();
            }
        }, 1200L, 1200L);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NGVKsellerExpansion(this).register();
        }

        SellerCommand commandExecutor = new SellerCommand(this);
        if (getCommand("seller") != null) {
            getCommand("seller").setExecutor(commandExecutor);
            getCommand("seller").setTabCompleter(commandExecutor);
        }

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveData();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    public static NGVKseller getInstance() { return instance; }
    public Economy getEconomy() { return econ; }
    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public DailyLimitManager getDailyLimitManager() { return dailyLimitManager; }
    public DynamicPriceManager getDynamicPriceManager() { return dynamicPriceManager; }
    public SellManager getSellManager() { return sellManager; }
}