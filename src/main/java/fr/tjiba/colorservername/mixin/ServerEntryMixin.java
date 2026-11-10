package fr.tjiba.colorservername.mixin;

import fr.tjiba.colorservername.ServerNameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class ServerEntryMixin {
    @Unique private String colorservername$cachedName;
    @Unique private Component colorservername$cachedText;

    @Redirect(method = "extractContent", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V", ordinal = 0))
    private void colorservername$renderName(GuiGraphicsExtractor graphics, Font font, String name, int x, int y, int color) {
        if (!name.equals(colorservername$cachedName)) {
            colorservername$cachedName = name;
            colorservername$cachedText = ServerNameText.parse(name);
        }
        graphics.text(font, colorservername$cachedText, x, y, color);
    }

    @ModifyArg(method = "getNarration", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 0), index = 1)
    private Object[] colorservername$narrateName(Object[] args) {
        if (args.length > 0 && args[0] instanceof String name) args[0] = ServerNameText.parse(name);
        return args;
    }
}
