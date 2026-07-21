package site.deforce.owl_on_a_jump_rope.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import site.deforce.owl_on_a_jump_rope.Owl_on_a_jump_rope;
import site.deforce.owl_on_a_jump_rope.mixin.client.GameRendererInvoker;

/**
 * Drives the black-and-white "freakout" post-process chain that follows the nuke flash. Engages a few
 * ticks into the flash and holds through the bombardment before releasing the screen.
 */
public final class FreakoutEffect {

    private FreakoutEffect() {
    }

    private static final Identifier CHAIN =
            Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "freakout");

    private static final int APPLY_DELAY = 6;  // engage while the flash is still solid white
    // Hold until the apocalypse is spent. Keep in rough sync with DeathManager.BOMBARDMENT_TICKS (+margin)
    // so the screen only clears once the last blast has gone off.
    private static final int DURATION = 420;

    private static int elapsed = -1; // -1 == inactive

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FreakoutEffect::tick);
    }

    public static void start() {
        elapsed = 0;
        CameraShake.start(DURATION);
    }

    /** Immediately drop the post-process chain and go dormant (used when an uno-reverse cancels the doom). */
    public static void stop() {
        elapsed = -1;
        Minecraft.getInstance().gameRenderer.clearPostEffect();
    }

    private static void tick(Minecraft client) {
        if (elapsed < 0) {
            return;
        }
        elapsed++;

        if (elapsed >= DURATION) {
            client.gameRenderer.clearPostEffect();
            elapsed = -1;
            return;
        }

        // Re-assert every tick past the delay: a camera/dimension change can quietly drop the
        // active chain, and reasserting an already-set effect is a no-op.
        if (elapsed >= APPLY_DELAY && !CHAIN.equals(client.gameRenderer.currentPostEffect())) {
            ((GameRendererInvoker) client.gameRenderer).owl$setPostEffect(CHAIN);
        }
    }
}
