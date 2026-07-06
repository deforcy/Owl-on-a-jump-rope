package site.deforce.owl_on_a_jump_rope.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import site.deforce.owl_on_a_jump_rope.client.Blackout;

/**
 * Refuses to start any sound while a blackout is active. {@code Blackout.stop()} only kills sounds
 * after they begin (a one-tick "beat"); blocking them here at the source makes the silence total.
 */
@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At("HEAD"), cancellable = true)
    private void owl$muteWhileBlackout(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (Blackout.active()) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
