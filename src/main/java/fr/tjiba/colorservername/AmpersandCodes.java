package fr.tjiba.colorservername;

public final class AmpersandCodes {
    private static final char SECTION = '\u00a7';
    private static final String CODES = "0123456789abcdefklmnor";

    private AmpersandCodes() {}

    private static boolean isCode(char c) {
        return CODES.indexOf(Character.toLowerCase(c)) >= 0;
    }

    static int codeLength(String text, int index) {
        if (index + 1 >= text.length()) return 0;
        if (text.charAt(index) != '&' && text.charAt(index) != SECTION) return 0;
        char code = text.charAt(index + 1);
        if (isCode(code)) return 2;
        if (code != '#' || index + 8 > text.length()) return 0;
        for (int i = index + 2; i < index + 8; i++) {
            if (Character.digit(text.charAt(i), 16) < 0) return 0;
        }
        return 8;
    }

    public static String toSection(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : 0;
            if (c != '&') {
                out.append(c);
                continue;
            }
            if (next == '&') {
                out.append('&');
                i++;
                continue;
            }
            int length = codeLength(text, i);
            if (length > 0) {
                out.append(SECTION).append(text, i + 1, i + length);
                i += length - 1;
                continue;
            }
            out.append('&');
        }
        return out.toString();
    }

    public static String toAmpersand(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&') {
                out.append("&&");
                continue;
            }
            int length = codeLength(text, i);
            if (c == SECTION && length > 0) {
                out.append('&').append(text, i + 1, i + length);
                i += length - 1;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    public record Edit(String text, int start, int end) {}

    public static Edit applyCode(String text, int cursor, int highlight, char code, int maxLength) {
        code = Character.toLowerCase(code);
        if (!isCode(code)) return null;
        return applyCode(text, cursor, highlight, "&" + code, maxLength);
    }

    public static Edit applyHex(String text, int cursor, int highlight, int color, int maxLength) {
        if ((color & 0xff000000) != 0) return null;
        return applyCode(text, cursor, highlight, hexCode(color), maxLength);
    }

    private static Edit applyCode(String text, int cursor, int highlight, String code, int maxLength) {
        int start = boundary(text, Math.min(cursor, highlight), cursor == highlight);
        int end = boundary(text, Math.max(cursor, highlight), true);
        String selected = formatSelection(text.substring(start, end), code);
        return replace(text, start, end, selected, code.length(), maxLength);
    }

    private static Edit replace(String text, int start, int end, String selected, int prefixLength, int maxLength) {
        String before = text.substring(0, start);
        int ampersands = 0;
        for (int i = before.length() - 1; i >= 0 && before.charAt(i) == '&'; i--) ampersands++;
        // Un & isolé ne doit pas absorber le nouveau code en formant un échappement &&.
        if (ampersands % 2 != 0) before += '&';
        String after = text.substring(end);
        String restore = start < end && !after.isEmpty() ? styleAt(text, end) : "";
        if (codeLength(after, 0) > 0 && after.charAt(0) == '&' && isColorOrReset(after.charAt(1))) restore = "";

        String result = before + selected + restore + after;
        if (result.length() > maxLength) return null;
        return new Edit(result, before.length() + prefixLength, before.length() + selected.length());
    }

    public static Edit applyGradient(String text, int cursor, int highlight, int first, int last, int maxLength) {
        if (((first | last) & 0xff000000) != 0) return null;
        int start = boundary(text, Math.min(cursor, highlight), false);
        int end = boundary(text, Math.max(cursor, highlight), true);
        if (cursor == highlight) return null;
        String selected = text.substring(start, end);
        int count = visibleLength(selected);
        if (count == 0) return null;

        String initial = styleAt(text, start);
        StringBuilder styles = new StringBuilder(initial.substring(codeLength(initial, 0)));
        StringBuilder out = new StringBuilder();
        int character = 0;
        for (int i = 0; i < selected.length();) {
            int length = tokenLength(selected, i);
            boolean escaped = selected.startsWith("&&", i);
            if (!escaped && selected.charAt(i) == '&' && codeLength(selected, i) > 0) {
                char code = Character.toLowerCase(selected.charAt(i + 1));
                if (isColorOrReset(code)) styles.setLength(0);
                else if (styles.indexOf("&" + code) < 0) styles.append('&').append(code);
                i += length;
                continue;
            }
            out.append(hexCode(interpolate(first, last, character++, count))).append(styles);
            if (selected.charAt(i) == '&' && length == 1) out.append("&&");
            else out.append(selected, i, i + length);
            i += length;
        }
        return replace(text, start, end, out.toString(), 8, maxLength);
    }

    public static int visibleLength(String text) {
        int count = 0;
        for (int i = 0; i < text.length();) {
            if (text.startsWith("&&", i) || text.charAt(i) != '&' || codeLength(text, i) == 0) count++;
            i += tokenLength(text, i);
        }
        return count;
    }

    private static String hexCode(int color) {
        String value = Integer.toHexString(color);
        return "&#" + "0".repeat(6 - value.length()) + value;
    }

    private static int interpolate(int first, int last, int index, int count) {
        if (count == 1) return first;
        int color = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            int from = (first >> shift) & 255;
            int to = (last >> shift) & 255;
            color |= Math.round(from + (to - from) * (float) index / (count - 1)) << shift;
        }
        return color;
    }

    private static int tokenLength(String text, int index) {
        if (text.startsWith("&&", index)) return 2;
        int length = text.charAt(index) == '&' ? codeLength(text, index) : 0;
        return length > 0 ? length : Character.charCount(text.codePointAt(index));
    }

    private static int boundary(String text, int position, boolean forward) {
        for (int i = 0; i < text.length();) {
            int length = tokenLength(text, i);
            if (position > i && position < i + length) return forward ? i + length : i;
            i += length;
        }
        return position;
    }

    private static boolean isColorOrReset(char code) {
        return "0123456789abcdefr#".indexOf(Character.toLowerCase(code)) >= 0;
    }

    private static String formatSelection(String text, String format) {
        char code = format.charAt(1);
        StringBuilder out = new StringBuilder(format);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? Character.toLowerCase(text.charAt(i + 1)) : 0;
            if (c == '&' && next == '&') {
                out.append("&&");
                i++;
                continue;
            }
            int length = codeLength(text, i);
            if (c != '&' || length == 0) {
                if (c == '&') out.append("&&");
                else out.append(c);
                continue;
            }
            String token = text.substring(i, i + length);
            i += length - 1;
            if (code == 'r' || (!isColorOrReset(code) && next == code)) continue;
            if (isColorOrReset(code) && isColorOrReset(next)) {
                if (out.length() > format.length()) out.append(format);
                continue;
            }
            out.append(token);
            // Une couleur vanilla efface les styles : réappliquer le style choisi dans la sélection.
            if (isColorOrReset(next)) out.append(format);
        }
        return out.toString();
    }

    private static String styleAt(String text, int end) {
        StringBuilder style = new StringBuilder("&r");
        for (int i = 0; i + 1 < end; i++) {
            if (text.charAt(i) != '&') continue;
            char code = Character.toLowerCase(text.charAt(i + 1));
            if (code == '&') {
                i++;
                continue;
            }
            int length = codeLength(text, i);
            if (length == 0 || i + length > end) continue;
            String token = text.substring(i, i + length);
            i += length - 1;
            if (isColorOrReset(code)) style.setLength(0);
            if (style.indexOf(token) < 0) style.append(token);
        }
        return style.toString();
    }
}
