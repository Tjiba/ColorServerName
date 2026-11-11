package fr.tjiba.colorservername;

import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StyledNameTest {
    private List<Style> styles(StyledName name) {
        List<Style> result = new ArrayList<>();
        name.preview().visit((style, text) -> {
            text.codePoints().forEach(character -> result.add(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    @Test
    void opensExistingGradientAsPlainTextAndRoundTrips() {
        StyledName name = new StyledName("§#ff0000§lA§#0000ff§oB");
        assertEquals("AB", name.text());
        StyledName reopened = new StyledName(name.stored());
        assertEquals(name.text(), reopened.text());
        assertEquals(styles(name), styles(reopened));
    }

    @Test
    void boldTogglesAcrossGradientWithoutSelection() {
        StyledName name = new StyledName("ABC");
        name.color(0, 0, 0xff0000, 0x0000ff);
        var colors = styles(name).stream().map(Style::getColor).toList();
        name.format('l', 1, 1);
        assertTrue(name.active('l', 0, 0));
        assertEquals(colors, styles(name).stream().map(Style::getColor).toList());
        name.format('l', 1, 1);
        assertFalse(name.active('l', 0, 0));
        assertTrue(styles(name).stream().noneMatch(Style::isBold));
    }

    @Test
    void colorsPreserveStylesInEitherOrder() {
        StyledName first = new StyledName("Hello");
        first.format('l', 0, 0);
        first.format('o', 0, 0);
        first.color(0, 0, 0xff0000, 0x0000ff);
        assertTrue(styles(first).stream().allMatch(style -> style.isBold() && style.isItalic()));
        first.format('a', 0, 0);
        assertTrue(styles(first).stream().allMatch(Style::isBold));
        assertEquals(0x55ff55, styles(first).getFirst().getColor().getValue());
    }

    @Test
    void changesOnlySelectedWordAndResetsOnlySelection() {
        StyledName name = new StyledName("Hi there");
        name.color(0, 0, 0x123456, 0xabcdef);
        name.format('l', 2, 0);
        assertTrue(name.active('l', 0, 2));
        assertFalse(styles(name).get(3).isBold());
        name.format('r', 0, 2);
        assertNull(styles(name).get(0).getColor());
        assertNotNull(styles(name).get(3).getColor());
    }

    @Test
    void typingAndDeletingPreserveUnaffectedStyles() {
        StyledName name = new StyledName("§cA§9B");
        name.setText("AXB", 2);
        assertEquals(0xff5555, styles(name).get(0).getColor().getValue());
        assertEquals(0x5555ff, styles(name).get(2).getColor().getValue());
        name.setText("AB!", 3);
        assertEquals(0x5555ff, styles(name).get(2).getColor().getValue());
        name.setText("", 0);
        name.setText("New", 3);
        assertEquals("New", new StyledName(name.stored()).text());
    }

    @Test
    void ampersandsAndHexLikeTextStayLiteralWhenEscaped() {
        StyledName name = new StyledName("");
        name.setEditorText("A && B &&#ff0000", 16);
        name.color(0, 0, 0xff0000, 0x0000ff);
        name.format('l', 0, 0);
        assertEquals("A & B &#ff0000", new StyledName(name.stored()).text());
    }

    @Test
    void selectionAndEditingKeepEmojiIntact() {
        StyledName name = new StyledName("A😀B");
        name.format('l', 1, 3);
        assertFalse(styles(name).get(0).isBold());
        assertTrue(styles(name).get(1).isBold());
        assertFalse(styles(name).get(2).isBold());
        name.setText("A😀!B", 4);
        assertEquals("A😀!B", new StyledName(name.stored()).text());
    }

    @Test
    void dialogCopyDoesNotChangeOriginalUntilApplied() {
        StyledName name = new StyledName("Hello");
        StyledName draft = name.copy();
        draft.color(0, 0, 0xff0000, 0x0000ff);
        assertNull(styles(name).get(0).getColor());
        assertEquals(0xff0000, styles(draft).get(0).getColor().getValue());
    }

    @Test
    void deletingFirstOfTwoIdenticalLettersKeepsSecondColor() {
        StyledName name = new StyledName("§cA§9A");
        name.setText("A", 0);
        assertEquals("§r§9A", name.stored());
    }

    @Test
    void paletteAndStylesRemainVisibleInEditor() {
        StyledName name = new StyledName("Server");
        var color = AmpersandCodes.applyCode(name.editorText(), 0, 0, 'c', 512);
        assertEquals("&cServer", color.text());
        name.setEditorText(color.text(), color.end());
        assertEquals("&cServer", name.editorText());
        var bold = AmpersandCodes.applyCode(color.text(), 2, 2, 'l', 512);
        name.setEditorText(bold.text(), bold.end());
        assertEquals("&c&lServer", name.editorText());
        assertTrue(styles(name).stream().allMatch(Style::isBold));
    }

    @Test
    void boldCodeWorksAcrossHiddenGradientAndSurvivesReopening() {
        StyledName name = new StyledName("Server");
        name.color(0, 0, 0xff0000, 0x0000ff);
        var colors = styles(name).stream().map(Style::getColor).toList();
        assertEquals("Server", name.editorText());
        name.setEditorText("&lServer", 2);
        assertEquals("&lServer", name.editorText());
        assertTrue(styles(name).stream().allMatch(Style::isBold));
        assertEquals(colors, styles(name).stream().map(Style::getColor).toList());
        StyledName reopened = new StyledName(name.stored());
        assertEquals("&lServer", reopened.editorText());
        assertEquals(styles(name), styles(reopened));
        name.setEditorText("Server", 0);
        assertTrue(styles(name).stream().noneMatch(Style::isBold));
        assertEquals(colors, styles(name).stream().map(Style::getColor).toList());
    }

    @Test
    void gradientKeepsVisibleStylesAndSolidHexRemainsVisible() {
        StyledName name = new StyledName("§lServer");
        name.color(0, 0, 0xff0000, 0x0000ff);
        assertEquals("&lServer", name.editorText());
        name.color(0, 0, 0x123456, 0x123456);
        assertEquals("&#123456&lServer", name.editorText());
        name.setEditorText(name.editorText(), 0);
        assertEquals(0x123456, styles(name).getFirst().getColor().getValue());
    }

    @Test
    void formattingSelectionKeepsGradientOutsideSelection() {
        StyledName name = new StyledName("ABCD");
        name.color(0, 0, 0xff0000, 0x0000ff);
        var colors = styles(name).stream().map(Style::getColor).toList();
        var edit = AmpersandCodes.applyCode(name.editorText(), 1, 3, 'l', 512);
        name.setEditorText(edit.text(), edit.end());
        assertFalse(styles(name).get(0).isBold());
        assertTrue(styles(name).get(1).isBold());
        assertTrue(styles(name).get(2).isBold());
        assertFalse(styles(name).get(3).isBold());
        assertEquals(colors, styles(name).stream().map(Style::getColor).toList());
    }

    @Test
    void rawPositionsHandleCodesEscapesAndEmoji() {
        String raw = "&lA&&😀&#123456B";
        assertEquals(0, StyledName.plainPosition(raw, 2));
        assertEquals(2, StyledName.plainPosition(raw, 5));
        assertEquals(4, StyledName.plainPosition(raw, 7));
        assertEquals(4, StyledName.plainPosition(raw, 15));
        assertEquals(5, StyledName.plainPosition(raw, raw.length()));
    }
    @Test
    void separateSolidHexWordsAreNotHiddenAsGradient() {
        StyledName name = new StyledName("§#123456Alpha §#abcdefBeta");
        assertEquals("&#123456Alpha &r&#ABCDEFBeta", name.editorText());
    }

}
