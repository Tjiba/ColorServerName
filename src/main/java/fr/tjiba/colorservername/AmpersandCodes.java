package fr.tjiba.colorservername;

public final class AmpersandCodes {
    private static final char SECTION = '\u00a7';
    private static final String CODES = "0123456789abcdefklmnor";

    private AmpersandCodes() {}

    private static boolean isCode(char c) {
        return CODES.indexOf(Character.toLowerCase(c)) >= 0;
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
            if (isCode(next)) {
                out.append(SECTION).append(next);
                i++;
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
            char next = i + 1 < text.length() ? text.charAt(i + 1) : 0;
            if (c == '&') {
                out.append("&&");
                continue;
            }
            if (c == SECTION && isCode(next)) {
                out.append('&').append(next);
                i++;
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

        int start = boundary(text, Math.min(cursor, highlight), cursor == highlight);
        int end = boundary(text, Math.max(cursor, highlight), true);
        String selected = formatSelection(text.substring(start, end), code);
        String after = text.substring(end);
        String restore = start < end && !after.isEmpty() ? styleAt(text, end) : "";
        if (after.length() >= 2 && after.charAt(0) == '&' && isColorOrReset(after.charAt(1))) restore = "";

        String result = text.substring(0, start) + selected + restore + after;
        if (result.length() > maxLength) return null;
        return new Edit(result, start + 2, start + selected.length());
    }

    private static int boundary(String text, int position, boolean forward) {
        for (int i = 0; i < text.length();) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : 0;
            int length = (c == '&' && (next == '&' || isCode(next))) || Character.isSurrogatePair(c, next) ? 2 : 1;
            if (position > i && position < i + length) return forward ? i + length : i;
            i += length;
        }
        return position;
    }

    private static boolean isColorOrReset(char code) {
        return "0123456789abcdefr".indexOf(Character.toLowerCase(code)) >= 0;
    }

    private static String formatSelection(String text, char code) {
        StringBuilder out = new StringBuilder("&" + code);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? Character.toLowerCase(text.charAt(i + 1)) : 0;
            if (c == '&' && next == '&') {
                out.append("&&");
                i++;
                continue;
            }
            if (c != '&' || !isCode(next)) {
                out.append(c);
                continue;
            }
            i++;
            if (code == 'r' || (!isColorOrReset(code) && next == code)) continue;
            if (isColorOrReset(code) && isColorOrReset(next)) {
                if (out.length() > 2) out.append('&').append(code);
                continue;
            }
            out.append('&').append(next);
            // Une couleur vanilla efface les styles : réappliquer le style choisi dans la sélection.
            if (isColorOrReset(next)) out.append('&').append(code);
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
            if (!isCode(code)) continue;
            i++;
            if (isColorOrReset(code)) style.setLength(0);
            if (style.indexOf("&" + code) < 0) style.append('&').append(code);
        }
        return style.toString();
    }
}
