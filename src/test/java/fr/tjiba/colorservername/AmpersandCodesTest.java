package fr.tjiba.colorservername;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AmpersandCodesTest {

    @Test
    void convertsCodes() {
        assertEquals("§4Test", AmpersandCodes.toSection("&4Test"));
        assertEquals("§c§lHyp§rixel", AmpersandCodes.toSection("&c&lHyp&rixel"));
        assertEquals("§LTest", AmpersandCodes.toSection("&LTest"));
    }

    @Test
    void escapesLiteralAmpersand() {
        assertEquals("&4Test", AmpersandCodes.toSection("&&4Test"));
        assertEquals("Tom & Jerry", AmpersandCodes.toSection("Tom & Jerry"));
        assertEquals("Tom &z", AmpersandCodes.toSection("Tom &z"));
        assertEquals("Tom &", AmpersandCodes.toSection("Tom &"));
    }

    @Test
    void roundTrips() {
        for (String name : new String[] { "&4Test", "&&4Test", "Tom && Jerry", "&c&lHyp&rixel", "Plain" }) {
            assertEquals(name, AmpersandCodes.toAmpersand(AmpersandCodes.toSection(name)));
        }
    }

    @Test
    void showsStoredSectionAsAmpersand() {
        assertEquals("&4Test", AmpersandCodes.toAmpersand("§4Test"));
        assertEquals("A && B", AmpersandCodes.toAmpersand("A & B"));
    }

    @Test
    void insertsAtCursor() {
        assertEquals(new AmpersandCodes.Edit("Hi &cworld", 5, 5),
            AmpersandCodes.applyCode("Hi world", 3, 3, 'c', 32));
    }

    @Test
    void formatsSelectionInBothDirections() {
        AmpersandCodes.Edit expected = new AmpersandCodes.Edit("&bHello&r world", 2, 7);
        assertEquals(expected, AmpersandCodes.applyCode("Hello world", 0, 5, 'b', 32));
        assertEquals(expected, AmpersandCodes.applyCode("Hello world", 5, 0, 'b', 32));
    }

    @Test
    void restoresSurroundingColorAndStyles() {
        assertEquals("&6&l&bHello&6&l world",
            AmpersandCodes.applyCode("&6&lHello world", 4, 9, 'b', 32).text());
    }

    @Test
    void usesExistingResetAfterSelection() {
        assertEquals("&aHello&7 world",
            AmpersandCodes.applyCode("Hello&7 world", 0, 5, 'a', 16).text());
    }

    @Test
    void recolorsExistingCodesInsideSelection() {
        assertEquals("&b&lOne &bTwo&7!",
            AmpersandCodes.applyCode("&6&lOne &7Two!", 0, 13, 'b', 32).text());
        assertEquals("&6&lOne &6Two",
            AmpersandCodes.applyCode("&6&lOne &6Two", 0, 13, '6', 32).text());
    }

    @Test
    void keepsChosenStyleAcrossColorChanges() {
        assertEquals("&l&a&lOne &b&lTwo",
            AmpersandCodes.applyCode("&aOne &bTwo", 0, 11, 'l', 32).text());
    }

    @Test
    void resetsOnlySelectedFormatting() {
        assertEquals("&rOne Two&b!",
            AmpersandCodes.applyCode("&a&lOne &bTwo!", 0, 13, 'r', 32).text());
    }

    @Test
    void preservesEscapedAmpersandsInsideSelection() {
        assertEquals("&cTom &&a Jerry",
            AmpersandCodes.applyCode("Tom &&a Jerry", 0, 13, 'c', 32).text());
        assertEquals("&&a &lHi&r!",
            AmpersandCodes.applyCode("&&a Hi!", 4, 6, 'l', 32).text());
    }

    @Test
    void doesNotSplitCodesOrEscapedAmpersands() {
        assertEquals(new AmpersandCodes.Edit("&a&bTest", 4, 4),
            AmpersandCodes.applyCode("&aTest", 1, 1, 'b', 32));
        assertEquals("&b&&&r!",
            AmpersandCodes.applyCode("&&!", 0, 1, 'b', 32).text());
    }

    @Test
    void doesNotSplitSurrogatePairs() {
        assertEquals(new AmpersandCodes.Edit("😀&b!", 4, 4),
            AmpersandCodes.applyCode("😀!", 1, 1, 'b', 32));
    }

    @Test
    void rejectsOverflowWithoutTruncatingText() {
        assertNull(AmpersandCodes.applyCode("x".repeat(31), 31, 31, 'c', 32));
        assertNull(AmpersandCodes.applyCode("x".repeat(29), 0, 1, 'c', 32));
        assertEquals("x".repeat(30) + "&c",
            AmpersandCodes.applyCode("x".repeat(30), 30, 30, 'c', 32).text());
    }

    @Test
    void validatesAndNormalizesPaletteCode() {
        assertNull(AmpersandCodes.applyCode("Test", 0, 0, 'z', 32));
        assertEquals("&aTest", AmpersandCodes.applyCode("Test", 0, 0, 'A', 32).text());
    }

    @Test
    void convertsHexAndEscapesLiteralHex() {
        assertEquals("§#12aBcFTest &##bad &#ffffff", AmpersandCodes.toSection("&#12aBcFTest &##bad &&#ffffff"));
        for (String name : new String[] { "&#ff55ffTest", "&&#ffffff", "&l&#000000Hi" }) {
            assertEquals(name, AmpersandCodes.toAmpersand(AmpersandCodes.toSection(name)));
        }
        for (String literal : new String[] { "&#12345", "&#12345z" }) {
            String stored = AmpersandCodes.toSection(literal);
            assertEquals(literal, stored);
            assertEquals(stored, AmpersandCodes.toSection(AmpersandCodes.toAmpersand(stored)));
        }
    }

    @Test
    void appliesHexAndRestoresSurroundingHex() {
        String name = "&#123456&lHi there";
        assertEquals("&#123456&l&#abcdefHi&#123456&l there",
            AmpersandCodes.applyHex(name, 10, 12, 0xabcdef, 512).text());
        assertEquals("&#000001Hi", AmpersandCodes.applyHex("Hi", 0, 2, 1, 512).text());
        assertNull(AmpersandCodes.applyHex("Hi", 0, 2, -1, 512));
    }

    @Test
    void paletteWorksAcrossHexCodes() {
        String name = "&#ff0000A&#0000ffB";
        assertEquals("&aA&aB", AmpersandCodes.applyCode(name, 0, name.length(), 'a', 512).text());
        assertEquals("&rAB", AmpersandCodes.applyCode(name, 0, name.length(), 'r', 512).text());
        assertEquals("&l&#ff0000&lA&#0000ff&lB", AmpersandCodes.applyCode(name, 0, name.length(), 'l', 512).text());
        assertEquals(new AmpersandCodes.Edit("&#ff0000&aHi", 10, 10),
            AmpersandCodes.applyCode("&#ff0000Hi", 4, 4, 'a', 512));
    }

    @Test
    void gradientHasExactEndpointsAndRoundedMidpoint() {
        assertEquals("&#ff0000A&#800080B&#0000ffC",
            AmpersandCodes.applyGradient("ABC", 0, 3, 0xff0000, 0x0000ff, 512).text());
        assertEquals("&#123456X", AmpersandCodes.applyGradient("X", 0, 1, 0x123456, 0xffffff, 512).text());
    }

    @Test
    void gradientPreservesStylesEmojiAndLiteralAmpersand() {
        String name = "&lA😀&&";
        assertEquals("&#ff0000&lA&#800080&l😀&#0000ff&l&&",
            AmpersandCodes.applyGradient(name, 0, name.length(), 0xff0000, 0x0000ff, 512).text());
        assertEquals(3, AmpersandCodes.visibleLength(name));
    }

    @Test
    void gradientRespectsSelectionAndRestoresTrailingStyle() {
        String name = "&6&lAB!";
        AmpersandCodes.Edit edit = AmpersandCodes.applyGradient(name, 6, 4, 0, 0xffffff, 512);
        assertEquals("&6&l&#000000&lA&#ffffff&lB&6&l!", edit.text());
        assertEquals(edit, AmpersandCodes.applyGradient(name, 4, 6, 0, 0xffffff, 512));
    }

    @Test
    void reapplyingGradientReplacesExistingColors() {
        String name = "&cA&9B";
        String first = AmpersandCodes.applyGradient(name, 0, name.length(), 0, 0xffffff, 512).text();
        assertEquals("&#000000A&#ffffffB", first);
        assertEquals("&#ffffffA&#000000B",
            AmpersandCodes.applyGradient(first, 0, first.length(), 0xffffff, 0, 512).text());
    }

    @Test
    void gradientRejectsEmptySelectionAndOverflow() {
        assertNull(AmpersandCodes.applyGradient("ABC", 1, 1, 0, 0xffffff, 512));
        assertNull(AmpersandCodes.applyGradient("&l", 0, 2, 0, 0xffffff, 512));
        assertNull(AmpersandCodes.applyGradient("ABC", 0, 3, 0, 0xffffff, 26));
        assertNull(AmpersandCodes.applyHex("ABC", 0, 3, 0xffffff, 10));
        assertNull(AmpersandCodes.applyGradient("ABC", 0, 3, -1, 0xffffff, 512));
    }
}
