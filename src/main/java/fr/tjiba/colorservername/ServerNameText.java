package fr.tjiba.colorservername;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class ServerNameText {
    private ServerNameText() {}

    public static Component parse(String name) {
        MutableComponent result = Component.empty();
        Style style = Style.EMPTY;
        int start = 0;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) != '\u00a7') continue;
            int length = AmpersandCodes.codeLength(name, i);
            if (length == 0) continue;
            if (start < i) result.append(Component.literal(name.substring(start, i)).setStyle(style));
            if (length == 8) {
                style = Style.EMPTY.withColor(Integer.parseInt(name.substring(i + 2, i + 8), 16));
            } else {
                ChatFormatting format = ChatFormatting.getByCode(name.charAt(i + 1));
                if (format != null) style = style.applyLegacyFormat(format);
            }
            i += length - 1;
            start = i + 1;
        }
        if (start < name.length()) result.append(Component.literal(name.substring(start)).setStyle(style));
        return result;
    }
}
