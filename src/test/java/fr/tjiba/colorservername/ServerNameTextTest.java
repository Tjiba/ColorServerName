package fr.tjiba.colorservername;

import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerNameTextTest {
    private record Glyph(int codePoint, Style style) {}

    private List<Glyph> glyphs(String storedName) {
        List<Glyph> result = new ArrayList<>();
        ServerNameText.parse(storedName).visit((style, text) -> {
            text.codePoints().forEach(codePoint -> result.add(new Glyph(codePoint, style)));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    @Test
    void rendersLegacyAndHexWithCorrectStyleResets() {
        List<Glyph> text = glyphs("§aA§lB§#123456C§oD§rE");
        assertEquals(5, text.size());
        assertEquals(0x55ff55, text.get(0).style().getColor().getValue());
        assertTrue(text.get(1).style().isBold());
        assertEquals(0x123456, text.get(2).style().getColor().getValue());
        assertFalse(text.get(2).style().isBold());
        assertTrue(text.get(3).style().isItalic());
        assertNull(text.get(4).style().getColor());
        assertFalse(text.get(4).style().isItalic());
    }

    @Test
    void rendersGradientWithoutFormattingCodes() {
        String name = "&lA😀&&";
        String gradient = AmpersandCodes.applyGradient(name, 0, name.length(), 0xff0000, 0x0000ff, 512).text();
        String stored = AmpersandCodes.toSection(gradient);
        List<Glyph> text = glyphs(stored);
        assertEquals("A😀&", ServerNameText.parse(stored).getString());
        assertEquals(List.of(0xff0000, 0x800080, 0x0000ff), text.stream().map(g -> g.style().getColor().getValue()).toList());
        assertTrue(text.stream().allMatch(g -> g.style().isBold()));
    }

    @Test
    void literalAmpersandsAndInvalidHexRemainVisible() {
        String editable = "&&#ff0000 &#bad &z";
        assertEquals("&#ff0000 &#bad &z", ServerNameText.parse(AmpersandCodes.toSection(editable)).getString());
    }

    @Test
    void gradientPreservesUnescapedLiteralAmpersands() {
        String name = "A & B &z &#12345";
        String gradient = AmpersandCodes.applyGradient(name, 0, name.length(), 0, 0xffffff, 512).text();
        assertEquals(name, ServerNameText.parse(AmpersandCodes.toSection(gradient)).getString());
        String saved = AmpersandCodes.toSection(gradient);
        assertEquals(saved, AmpersandCodes.toSection(AmpersandCodes.toAmpersand(saved)));
    }

    @Test
    void formattingPreservesVisibleTextAtEverySelectionBoundary() {
        for (String name : new String[] { "A &!", "A &&!", "A &&&!", "A &z!", "&#ff0000A&&B", "A😀B" }) {
            String expected = ServerNameText.parse(AmpersandCodes.toSection(name)).getString();
            for (int start = 0; start <= name.length(); start++) {
                for (int end = start; end <= name.length(); end++) {
                    AmpersandCodes.Edit solid = AmpersandCodes.applyHex(name, start, end, 0x123456, 1024);
                    assertEquals(expected, ServerNameText.parse(AmpersandCodes.toSection(solid.text())).getString(), name + " " + start + ":" + end);
                    AmpersandCodes.Edit gradient = AmpersandCodes.applyGradient(name, start, end, 0xff0000, 0x0000ff, 1024);
                    if (gradient != null) {
                        assertEquals(expected, ServerNameText.parse(AmpersandCodes.toSection(gradient.text())).getString(), name + " " + start + ":" + end);
                    }
                }
            }
        }
    }

    @Test
    void roundTripsGradientThroughServerData() {
        SharedConstants.tryDetectVersion();
        String name = "My Server";
        String gradient = AmpersandCodes.applyGradient(name, 0, name.length(), 0xff55ff, 0x55ffff, 512).text();
        ServerData original = new ServerData(AmpersandCodes.toSection(gradient), "example.org", ServerData.Type.OTHER);
        ServerData restored = ServerData.read(original.write());
        assertEquals(original.name, restored.name);
        assertEquals(gradient, AmpersandCodes.toAmpersand(restored.name));
        assertEquals(name, ServerNameText.parse(restored.name).getString());
    }

    @Test
    void validatesHexFieldInput() {
        assertEquals(0xff55aa, CustomColorScreen.parseHex("#FF55aA"));
        assertEquals(0, CustomColorScreen.parseHex("000000"));
        for (String invalid : new String[] { "", "#", "#12345", "#12345z", "#1234567", "ff ff ff" }) {
            assertNull(CustomColorScreen.parseHex(invalid));
        }
    }
}
