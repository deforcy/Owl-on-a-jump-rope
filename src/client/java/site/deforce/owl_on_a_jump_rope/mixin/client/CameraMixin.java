package site.deforce.owl_on_a_jump_rope.mixin.client;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import site.deforce.owl_on_a_jump_rope.client.CameraShake;

/**
 * Injecting at the tail of {@code alignWithEntity} (where vanilla sets the camera rotation/position
 * each frame) bakes our jitter into the view matrix and frustum, so the shake is real, not an overlay.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow protected abstract void move(float x, float y, float z);

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Shadow public abstract float xRot();

    @Shadow public abstract float yRot();

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void owl$shake(float partialTicks, CallbackInfo ci) {
        if (!CameraShake.active()) {
            return;
        }
        this.setRotation(this.yRot() + CameraShake.yawJitter(), this.xRot() + CameraShake.pitchJitter());
        this.move(CameraShake.moveJitter(), CameraShake.moveJitter(), CameraShake.moveJitter());
    }
}
