package fr.tjiba.colorservername.mixin;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Consumer;

@Mixin(EditBox.class)
public interface IEditBoxAccessor {
    @Accessor("highlightPos")
    int colorservername$getHighlightPos();

    @Accessor("responder")
    Consumer<String> colorservername$getResponder();
}
