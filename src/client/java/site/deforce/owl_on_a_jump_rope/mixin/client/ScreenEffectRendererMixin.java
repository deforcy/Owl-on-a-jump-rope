package site.deforce.owl_on_a_jump_rope.mixin.client;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import site.deforce.owl_on_a_jump_rope.ModItems;

/** Zeroes a card's random totem-animation entry offset so it rises straight up the middle. */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Shadow private float itemActivationOffX;
    @Shadow private float itemActivationOffY;

    @Inject(method = "displayItemActivation", at = @At("TAIL"))
    private void owl$center(ItemStack stack, RandomSource random, CallbackInfo ci) {
        if (stack.getItem() == ModItems.OWL_CARD || stack.getItem() == ModItems.UNO_REVERSE_CARD) {
            this.itemActivationOffX = 0.0F;
            this.itemActivationOffY = 0.0F;
        }
    }
}
