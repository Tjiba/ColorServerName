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
}
