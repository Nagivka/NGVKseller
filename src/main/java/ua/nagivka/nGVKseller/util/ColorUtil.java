package ua.nagivka.nNGVKseller.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private ColorUtil() {}

    public static Component format(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        Component component;
        if (text.contains("<") && text.contains(">")) {
            component = MINI_MESSAGE.deserialize(text);
        } else {
            String processed = text.replaceAll("(?<!&)#([a-fA-F0-9]{6})", "&#$1");
            component = LEGACY_SERIALIZER.deserialize(processed);
        }

        return component.decoration(TextDecoration.ITALIC, false);
    }
}