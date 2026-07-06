package site.deforce.owl_on_a_jump_rope.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import site.deforce.owl_on_a_jump_rope.client.NukeFlash;

/** Paints the nuke flash on top of any open screen (e.g. the death screen) so it shows there too. */
@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void owl_on_a_jump_rope$flashOverScreen(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                                                    float partialTick, CallbackInfo ci) {
        NukeFlash.render(extractor, partialTick);
    }
}
