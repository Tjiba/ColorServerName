package fr.tjiba.colorservername;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class CustomColorScreen extends Screen {
    private final StyledName name;
    private final int start;
    private final int end;
    private final Consumer<StyledName> onApply;
    private final Runnable onCancel;
    private EditBox firstHex;
    private EditBox lastHex;
    private Button colorButton;
    private Button gradientButton;
    private StyledName colorEdit;
    private StyledName gradientEdit;
    private Component colorPreview;
    private Component gradientPreview;
    private Component error;
    private int left;
    private int top;
    private int panelWidth;

    public CustomColorScreen(StyledName name, int cursor, int highlight,
                             Consumer<StyledName> onApply, Runnable onCancel) {
        super(Component.translatable("colorservername.custom"));
        this.name = name.copy();
        start = cursor == highlight ? 0 : Math.min(cursor, highlight);
        end = cursor == highlight ? name.text().length() : Math.max(cursor, highlight);
        this.onApply = onApply;
        this.onCancel = onCancel;
    }

    @Override
    protected void init() {
        String first = firstHex == null ? "#55ffff" : firstHex.getValue();
        String last = lastHex == null ? "#ff55ff" : lastHex.getValue();
        panelWidth = Math.min(244, width - 24);
        left = (width - panelWidth) / 2;
        top = Math.max(8, (height - 214) / 2);
        int half = (panelWidth - 4) / 2;
        firstHex = addRenderableWidget(new EditBox(font, left, top + 40, half, 20,
            Component.translatable("colorservername.hex_start")));
        lastHex = addRenderableWidget(new EditBox(font, left + half + 4, top + 40, half, 20,
            Component.translatable("colorservername.hex_end")));
        firstHex.setMaxLength(7);
        lastHex.setMaxLength(7);
        firstHex.setValue(first);
        lastHex.setValue(last);
        colorButton = addRenderableWidget(Button.builder(Component.translatable("colorservername.apply_color"),
                button -> onApply.accept(colorEdit)).bounds(left, top + 170, half, 20).build());
        gradientButton = addRenderableWidget(Button.builder(Component.translatable("colorservername.apply_gradient"),
                button -> onApply.accept(gradientEdit)).bounds(left + half + 4, top + 170, half, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
            .bounds(left, top + 194, panelWidth, 20).build());
        firstHex.setResponder(value -> updatePreview());
        lastHex.setResponder(value -> updatePreview());
        updatePreview();
        setInitialFocus(firstHex);
    }

    private void updatePreview() {
        Integer first = parseHex(firstHex.getValue());
        Integer last = parseHex(lastHex.getValue());
        boolean empty = name.text().isEmpty();
        colorEdit = first == null || empty ? null : name.copy();
        gradientEdit = first == null || last == null || empty ? null : name.copy();
        if (colorEdit != null) colorEdit.color(start, end, first, first);
        if (gradientEdit != null) gradientEdit.color(start, end, first, last);
        colorButton.active = colorEdit != null;
        gradientButton.active = gradientEdit != null;
        firstHex.setTextColor(first == null ? 0xffff5555 : 0xffe0e0e0);
        lastHex.setTextColor(last == null ? 0xffff5555 : 0xffe0e0e0);
        colorPreview = (colorEdit == null ? name : colorEdit).preview();
        gradientPreview = (gradientEdit == null ? name : gradientEdit).preview();
        error = null;
        if (empty) error = Component.translatable("colorservername.empty_name");
        else if (first == null || last == null) error = Component.translatable("colorservername.invalid_hex");
    }

    static Integer parseHex(String value) {
        String digits = value.startsWith("#") ? value.substring(1) : value;
        if (digits.length() != 6) return null;
        for (int i = 0; i < digits.length(); i++) {
            if (Character.digit(digits.charAt(i), 16) < 0) return null;
        }
        return Integer.parseInt(digits, 16);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, top, 0xffffffff);
        graphics.text(font, Component.translatable("colorservername.hex_start"), left + 1, top + 28, 0xffa0a0a0);
        graphics.text(font, Component.translatable("colorservername.hex_end"), lastHex.getX() + 1, top + 28, 0xffa0a0a0);
        preview(graphics, Component.translatable("colorservername.solid"), colorPreview, top + 84);
        preview(graphics, Component.translatable("colorservername.gradient"), gradientPreview, top + 124);
        if (error != null) graphics.text(font, error, left + 1, top + 152, 0xffff5555);
    }

    private void preview(GuiGraphicsExtractor graphics, Component label, Component text, int y) {
        graphics.text(font, label, left + 1, y - 12, 0xffa0a0a0);
        graphics.fill(left, y, left + panelWidth, y + 20, 0xff808080);
        graphics.fill(left + 1, y + 1, left + panelWidth - 1, y + 19, 0xff080808);
        graphics.enableScissor(left + 5, y + 1, left + panelWidth - 5, y + 19);
        graphics.text(font, text, left + 6, y + 6, 0xffffffff);
        graphics.disableScissor();
    }

    @Override
    public void onClose() {
        onCancel.run();
    }
}
