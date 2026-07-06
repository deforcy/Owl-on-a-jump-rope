package site.deforce.owl_on_a_jump_rope.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import site.deforce.owl_on_a_jump_rope.client.Blackout;

/**
 * During a blackout, replaces the death screen's red background with black. Targeting the background
 * layer keeps the "You Died" text and respawn buttons drawing on top.
 */
@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void owl$blackBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                     CallbackInfo ci) {
        if (Blackout.active()) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFF000000);
            ci.cancel();
        }
    }
}
