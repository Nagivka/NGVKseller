package ua.nagivka.nNGVKseller.config;

import org.bukkit.inventory.ItemStack;
import java.util.Map;

public record PageData(
        String title,
        int size,
        Map<Integer, ItemStack> items,
        Map<Integer, GuiItemData> itemDataMap
) {}