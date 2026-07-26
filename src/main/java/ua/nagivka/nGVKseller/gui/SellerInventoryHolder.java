package ua.nagivka.nNGVKseller.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class SellerInventoryHolder implements InventoryHolder {
    private final int page;

    public SellerInventoryHolder(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}