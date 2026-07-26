package ua.nagivka.nNGVKseller.config;

import org.bukkit.Material;

public record PriceData(
        Material material,
        double basePrice,
        double minPrice,
        double maxPrice,
        int customModelData
) {}