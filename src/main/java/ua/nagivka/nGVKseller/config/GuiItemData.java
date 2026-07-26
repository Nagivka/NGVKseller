package ua.nagivka.nNGVKseller.config;

import org.bukkit.Material;

public record GuiItemData(
        String action,
        Material targetMaterial
) {}