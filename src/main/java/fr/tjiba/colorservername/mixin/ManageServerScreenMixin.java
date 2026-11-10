package fr.tjiba.colorservername.mixin;

import fr.tjiba.colorservername.AmpersandCodes;
import fr.tjiba.colorservername.ColorButton;
import fr.tjiba.colorservername.CustomColorScreen;
import fr.tjiba.colorservername.ServerNameText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// EditBox.setValue ne passe pas par StringUtil.filterText (seuls insertText/charTyped filtrent),
// donc on injecte le § via setValue juste avant que onAdd lise getValue : aucun contournement du filtre.
@Mixin(ManageServerScreen.class)
public abstract class ManageServerScreenMixin extends Screen {

    @Shadow private EditBox nameEdit;
    @Shadow private EditBox ipEdit;
    @Shadow private Button addButton;
    @Shadow @Final private ServerData serverData;

    @Unique private int colorservername$titleY;
    @Unique private int colorservername$previewY;
    @Unique private int colorservername$previewHeight;
    @Unique private int colorservername$nameLimit;
    @Unique private boolean colorservername$focusName;
    @Unique private boolean colorservername$layoutPending;
    @Unique private String colorservername$limitValue;

    protected ManageServerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void colorservername$showAmpersands(CallbackInfo ci) {
        // Relire le nom complet : vanilla l'a déjà tronqué à 32 caractères dans init.
        String name = AmpersandCodes.toAmpersand(serverData.name);
        colorservername$nameLimit = Math.max(512, name.length());
        nameEdit.setMaxLength(colorservername$nameLimit);
        nameEdit.setValue(name);
        colorservername$limitValue = null;
        colorservername$focusName = false;
        colorservername$layoutPending = true;
    }

    @Unique
    private void colorservername$layout() {
        List<AbstractWidget> options = new ArrayList<>();
        Button cancelButton = null;
        for (GuiEventListener child : children()) {
            if (child == nameEdit || child == ipEdit || child == addButton) continue;
            if (child instanceof Button button && button.getMessage().equals(CommonComponents.GUI_CANCEL)) {
                cancelButton = button;
                continue;
            }
            if (child instanceof AbstractWidget widget) options.add(widget);
        }
        options.sort(Comparator.comparingInt(AbstractWidget::getY));

        int extraRows = Math.max(0, options.size() - 1);
        boolean compact = height < 300 + extraRows * 24;
        int rowHeight = compact ? 16 : 20;
        int fieldHeight = compact ? 16 : 20;
        int gap = compact ? 2 : 4;
        int fieldWidth = Math.min(244, width - 24);
        int left = (width - fieldWidth) / 2;
        int contentHeight = (compact ? 212 : 272) + extraRows * (fieldHeight + gap);
        colorservername$titleY = Math.max(4, (height - contentHeight) / 2);
        int nameY = colorservername$titleY + (compact ? 22 : 28);
        int colorsY = nameY + fieldHeight + gap;
        int stylesY = colorsY + 2 * (rowHeight + gap);
        colorservername$previewY = stylesY + 2 * (rowHeight + gap) + gap + 12;
        colorservername$previewHeight = compact ? 18 : 24;
        int ipY = colorservername$previewY + colorservername$previewHeight + 16;
        int packsY = ipY + fieldHeight + gap;
        int footerY = packsY + options.size() * (fieldHeight + gap);

        colorservername$move(nameEdit, left, nameY, fieldWidth, fieldHeight);
        colorservername$move(ipEdit, left, ipY, fieldWidth, fieldHeight);
        int footerWidth = (fieldWidth - gap) / 2;
        for (int i = 0; i < options.size(); i++) {
            colorservername$move(options.get(i), left, packsY + i * (fieldHeight + gap), fieldWidth, fieldHeight);
        }
        colorservername$move(addButton, left, footerY, footerWidth, fieldHeight);
        if (cancelButton != null) {
            colorservername$move(cancelButton, left + footerWidth + gap, footerY, footerWidth, fieldHeight);
        }

        for (int i = 0; i < 16; i++) {
            char code = "0123456789abcdef".charAt(i);
            int x = left + (i % 8) * (fieldWidth + gap) / 8;
            int right = left + (i % 8 + 1) * (fieldWidth + gap) / 8 - gap;
            addRenderableWidget(new ColorButton(x, colorsY + (i / 8) * (rowHeight + gap), right - x, rowHeight,
                code, () -> colorservername$insertCode(code)));
        }
        String styles = "lonmkr";
        for (int i = 0; i < styles.length(); i++) {
            char code = styles.charAt(i);
            Component label = Component.translatable("colorservername.style." + code);
            int x = left + (i % 3) * (fieldWidth + gap) / 3;
            int right = left + (i % 3 + 1) * (fieldWidth + gap) / 3 - gap;
            int buttonWidth = right - x;
            if (code == 'r') {
                addRenderableWidget(Button.builder(Component.literal("+"), button -> colorservername$openColors())
                    .bounds(right - rowHeight, stylesY + (i / 3) * (rowHeight + gap), rowHeight, rowHeight)
                    .tooltip(Tooltip.create(Component.translatable("colorservername.custom"))).build());
                buttonWidth -= rowHeight + gap;
            }
            addRenderableWidget(Button.builder(label, button -> colorservername$insertCode(code))
                .bounds(x, stylesY + (i / 3) * (rowHeight + gap), buttonWidth, rowHeight)
                .tooltip(Tooltip.create(label.copy().append(" (&" + code + ")")))
                .build());
        }
    }

    @Unique
    private static void colorservername$move(AbstractWidget widget, int x, int y, int width, int height) {
        widget.setWidth(width);
        widget.setHeight(height);
        widget.setX(x);
        widget.setY(y);
    }

    @Unique
    private void colorservername$insertCode(char code) {
        String value = nameEdit.getValue();
        int highlight = ((IEditBoxAccessor) nameEdit).colorservername$getHighlightPos();
        AmpersandCodes.Edit edit = AmpersandCodes.applyCode(value, nameEdit.getCursorPosition(), highlight, code, colorservername$nameLimit);
        colorservername$applyEdit(edit);
    }

    @Unique
    private void colorservername$applyEdit(AmpersandCodes.Edit edit) {
        colorservername$focusName = true;
        if (edit == null) {
            colorservername$limitValue = nameEdit.getValue();
            return;
        }
        nameEdit.setValue(edit.text());
        nameEdit.setCursorPosition(edit.end());
        nameEdit.setHighlightPos(edit.start());
        colorservername$limitValue = null;
    }

    @Unique
    private void colorservername$openColors() {
        String name = nameEdit.getValue();
        String ip = ipEdit.getValue();
        int cursor = nameEdit.getCursorPosition();
        int highlight = ((IEditBoxAccessor) nameEdit).colorservername$getHighlightPos();
        Runnable restore = () -> {
            minecraft.setScreenAndShow(this);
            nameEdit.setValue(name);
            ipEdit.setValue(ip);
            nameEdit.setCursorPosition(cursor);
            nameEdit.setHighlightPos(highlight);
            colorservername$focusName = true;
        };
        minecraft.setScreenAndShow(new CustomColorScreen(name, cursor, highlight, colorservername$nameLimit, edit -> {
            restore.run();
            colorservername$applyEdit(edit);
        }, restore));
    }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 3)
    private int colorservername$titlePosition(int y) {
        return colorservername$titleY;
    }

    @ModifyArgs(method = "extractRenderState", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", ordinal = 0))
    private void colorservername$nameLabelPosition(Args args) {
        args.set(2, nameEdit.getX() + 1);
        args.set(3, nameEdit.getY() - 12);
    }

    @ModifyArgs(method = "extractRenderState", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", ordinal = 1))
    private void colorservername$ipLabelPosition(Args args) {
        args.set(2, ipEdit.getX() + 1);
        args.set(3, ipEdit.getY() - 12);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void colorservername$restoreFocus(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Essential et d'autres mods ajoutent ou déplacent leurs boutons à la fin de init.
        if (colorservername$layoutPending) {
            colorservername$layoutPending = false;
            colorservername$layout();
        }
        if (!colorservername$focusName) return;
        // Le conteneur donne le focus au bouton après onPress ; le rendre au champ à la frame suivante.
        setFocused(nameEdit);
        colorservername$focusName = false;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void colorservername$preview(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int x = nameEdit.getX();
        int width = nameEdit.getWidth();
        String name = nameEdit.getValue();
        String counter = name.length() + " / " + colorservername$nameLimit;
        graphics.text(font, counter, x + width - font.width(counter), nameEdit.getY() - 12, 0xffa0a0a0);
        boolean limit = name.equals(colorservername$limitValue);
        Component label = limit ? Component.translatable("colorservername.limit", colorservername$nameLimit)
            : Component.translatable("colorservername.preview");
        graphics.text(font, label, x + 1, colorservername$previewY - 12, limit ? 0xffff5555 : 0xffa0a0a0);
        graphics.fill(x, colorservername$previewY, x + width, colorservername$previewY + colorservername$previewHeight, 0xff808080);
        graphics.fill(x + 1, colorservername$previewY + 1, x + width - 1, colorservername$previewY + colorservername$previewHeight - 1, 0xff080808);
        Component preview = name.isEmpty() ? Component.translatable("selectServer.defaultName")
            : ServerNameText.parse(AmpersandCodes.toSection(name));
        graphics.enableScissor(x + 5, colorservername$previewY + 1, x + width - 5, colorservername$previewY + colorservername$previewHeight - 1);
        graphics.text(font, preview, x + 6, colorservername$previewY + (colorservername$previewHeight - 9) / 2, 0xffffffff);
        graphics.disableScissor();
    }

    @Inject(method = "onAdd", at = @At("HEAD"))
    private void colorservername$applyCodes(CallbackInfo ci) {
        nameEdit.setValue(AmpersandCodes.toSection(nameEdit.getValue()));
    }
}
