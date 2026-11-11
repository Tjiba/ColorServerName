package fr.tjiba.colorservername;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StyledName {
    private record Glyph(int character, Style style, boolean gradient) {
        Glyph(int character, Style style) { this(character, style, false); }
    }
    private final List<Glyph> glyphs = new ArrayList<>();

    public StyledName(String stored) {
        this(stored, true);
    }

    private StyledName(String stored, boolean detectGradient) {
        ServerNameText.parse(stored).visit((style, text) -> {
            text.codePoints().forEach(character -> glyphs.add(new Glyph(character, style)));
            return Optional.empty();
        }, Style.EMPTY);
        // Les anciens noms stockent le dégradé comme une couleur HEX par caractère.
        if (!detectGradient || !stored.contains("§#")) return;
        int start = 0;
        while (start < glyphs.size()) {
            int end = start + 1;
            while (end < glyphs.size() && singleColorGlyph(end - 1) && singleColorGlyph(end)) end++;
            if (end - start > 1) {
                for (int i = start; i < end; i++) {
                    Glyph glyph = glyphs.get(i);
                    glyphs.set(i, new Glyph(glyph.character(), glyph.style(), true));
                }
            }
            start = end;
        }
    }

    private boolean singleColorGlyph(int index) {
        TextColor color = glyphs.get(index).style().getColor();
        if (color == null) return false;
        return (index == 0 || !color.equals(glyphs.get(index - 1).style().getColor()))
            && (index + 1 == glyphs.size() || !color.equals(glyphs.get(index + 1).style().getColor()));
    }

    public StyledName copy() {
        StyledName copy = new StyledName("");
        copy.glyphs.addAll(glyphs);
        return copy;
    }

    public String text() {
        StringBuilder text = new StringBuilder();
        for (Glyph glyph : glyphs) text.appendCodePoint(glyph.character());
        return text.toString();
    }

    public void setText(String text, int caret) {
        int[] next = text.codePoints().toArray();
        // Le curseur distingue les lettres identiques de part et d'autre de la modification.
        int tail = text.codePointCount(Math.clamp(caret, 0, text.length()), text.length());
        int suffix = 0;
        while (suffix < tail && suffix < glyphs.size()
            && next[next.length - 1 - suffix] == glyphs.get(glyphs.size() - 1 - suffix).character()) suffix++;
        int prefix = 0;
        while (prefix < next.length - suffix && prefix < glyphs.size() - suffix
            && next[prefix] == glyphs.get(prefix).character()) prefix++;
        boolean gradient = !glyphs.isEmpty() && glyphs.get(Math.min(prefix, glyphs.size() - 1)).gradient();
        Style inherited = glyphs.isEmpty() ? Style.EMPTY : glyphs.get(Math.min(prefix, glyphs.size() - 1)).style();
        glyphs.subList(prefix, glyphs.size() - suffix).clear();
        for (int i = prefix; i < next.length - suffix; i++) glyphs.add(i, new Glyph(next[i], inherited, gradient));
    }

    public static int plainPosition(String text, int position) {
        int end = Math.clamp(position, 0, text.length());
        int plain = 0;
        for (int i = 0; i < end;) {
            boolean escaped = text.startsWith("&&", i);
            int code = escaped ? 0 : AmpersandCodes.codeLength(text, i);
            int length = escaped ? 2 : code > 0 ? code : Character.charCount(text.codePointAt(i));
            if (i + length > end) break;
            if (code == 0) plain += escaped ? 1 : length;
            i += length;
        }
        return plain;
    }

    public void setEditorText(String value, int caret) {
        StyledName parsed = new StyledName(AmpersandCodes.toSection(value), false);
        setText(parsed.text(), plainPosition(value, caret));
        for (int i = 0; i < glyphs.size(); i++) {
            Glyph old = glyphs.get(i);
            Style style = parsed.glyphs.get(i).style();
            boolean gradient = old.gradient() && style.getColor() == null;
            if (gradient) style = style.withColor(old.style().getColor());
            glyphs.set(i, new Glyph(old.character(), style, gradient));
        }
    }

    public String editorText() {
        StyledName visible = new StyledName("");
        for (Glyph glyph : glyphs) {
            Style style = glyph.gradient() ? glyph.style().withColor((TextColor) null) : glyph.style();
            visible.glyphs.add(new Glyph(glyph.character(), style));
        }
        return AmpersandCodes.toAmpersand(visible.stored()).replaceFirst("^&r", "");
    }

    private int[] range(int cursor, int highlight) {
        if (cursor == highlight) return new int[] {0, glyphs.size()};
        String text = text();
        int start = Math.clamp(Math.min(cursor, highlight), 0, text.length());
        int end = Math.clamp(Math.max(cursor, highlight), 0, text.length());
        if (start > 0 && start < text.length() && Character.isLowSurrogate(text.charAt(start))) start--;
        if (end > 0 && end < text.length() && Character.isLowSurrogate(text.charAt(end))) end++;
        return new int[] {text.codePointCount(0, start), text.codePointCount(0, end)};
    }

    public boolean active(char code, int cursor, int highlight) {
        int[] range = range(cursor, highlight);
        if (range[0] == range[1]) return false;
        for (int i = range[0]; i < range[1]; i++) {
            Style style = glyphs.get(i).style();
            boolean active = switch (code) {
                case 'l' -> style.isBold();
                case 'o' -> style.isItalic();
                case 'n' -> style.isUnderlined();
                case 'm' -> style.isStrikethrough();
                case 'k' -> style.isObfuscated();
                default -> false;
            };
            if (!active) return false;
        }
        return true;
    }

    public void format(char code, int cursor, int highlight) {
        int[] range = range(cursor, highlight);
        boolean enabled = !active(code, cursor, highlight);
        for (int i = range[0]; i < range[1]; i++) {
            Glyph glyph = glyphs.get(i);
            Style style = glyph.style();
            style = switch (code) {
                case 'l' -> style.withBold(enabled);
                case 'o' -> style.withItalic(enabled);
                case 'n' -> style.withUnderlined(enabled);
                case 'm' -> style.withStrikethrough(enabled);
                case 'k' -> style.withObfuscated(enabled);
                case 'r' -> Style.EMPTY;
                default -> style.withColor(ChatFormatting.getByCode(code));
            };
            glyphs.set(i, new Glyph(glyph.character(), style, glyph.gradient() && "lonmk".indexOf(code) >= 0));
        }
    }

    public void color(int cursor, int highlight, int first, int last) {
        int[] range = range(cursor, highlight);
        int count = range[1] - range[0];
        for (int i = 0; i < count; i++) {
            int color = 0;
            for (int shift = 0; shift <= 16; shift += 8) {
                int from = (first >> shift) & 255;
                int to = (last >> shift) & 255;
                color |= Math.round(from + (to - from) * (count < 2 ? 0 : (float) i / (count - 1))) << shift;
            }
            Glyph glyph = glyphs.get(range[0] + i);
            glyphs.set(range[0] + i, new Glyph(glyph.character(), glyph.style().withColor(color), first != last));
        }
    }

    public Component preview() {
        MutableComponent result = Component.empty();
        for (Glyph glyph : glyphs) result.append(Component.literal(Character.toString(glyph.character())).setStyle(glyph.style()));
        return result;
    }

    public String stored() {
        StringBuilder result = new StringBuilder();
        Style previous = Style.EMPTY;
        for (Glyph glyph : glyphs) {
            Style style = glyph.style();
            if (!style.equals(previous)) {
                result.append("§r");
                if (style.getColor() != null) appendColor(result, style.getColor());
                if (style.isBold()) result.append("§l");
                if (style.isItalic()) result.append("§o");
                if (style.isUnderlined()) result.append("§n");
                if (style.isStrikethrough()) result.append("§m");
                if (style.isObfuscated()) result.append("§k");
                previous = style;
            }
            result.appendCodePoint(glyph.character());
        }
        return result.toString();
    }

    private static void appendColor(StringBuilder result, TextColor color) {
        for (ChatFormatting format : ChatFormatting.values()) {
            if (TextColor.fromLegacyFormat(format) != color) continue;
            result.append(format);
            return;
        }
        result.append('§').append(color.serialize());
    }
}
