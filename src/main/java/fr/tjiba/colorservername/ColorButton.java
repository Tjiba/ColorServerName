package fr.tjiba.colorservername;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public final class ColorButton extends Button {
    private static final int[] COLORS = {
        0x000000, 0x0000aa, 0x00aa00, 0x00aaaa, 0xaa0000, 0xaa00aa, 0xffaa00, 0xaaaaaa,
        0x555555, 0x5555ff, 0x55ff55, 0x55ffff, 0xff5555, 0xff55ff, 0xffff55, 0xffffff
    };

    private final int color;
    private final int textColor;

    public ColorButton(int x, int y, int width, int height, char code, Runnable onPress) {
        super(x, y, width, height, Component.literal("&" + code), button -> onPress.run(), DEFAULT_NARRATION);
        int index = Character.digit(code, 16);
        color = 0xff000000 | COLORS[index];
        textColor = index < 6 || index == 8 ? 0xffffffff : 0xff000000;
        setTooltip(Tooltip.create(Component.translatable("colorservername.color." + code).append(" (&" + code + ")")));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractDefaultSprite(graphics);
        graphics.fill(getX() + 2, getY() + 2, getRight() - 2, getBottom() - 2, color);
        if (isHoveredOrFocused()) graphics.outline(getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 0xffffffff);
        Font font = Minecraft.getInstance().font;
        graphics.text(font, getMessage(), getX() + (getWidth() - font.width(getMessage())) / 2,
            getY() + (getHeight() - 9) / 2, textColor, false);
    }
}
