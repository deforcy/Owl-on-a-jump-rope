package site.deforce.owl_on_a_jump_rope.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import site.deforce.owl_on_a_jump_rope.ModItems;

/**
 * Client half of the uno-reverse rescue: tears down every doom visual the server had going and plays
 * the uno-reverse card's totem animation in their place. The two stings that accompany it are driven
 * from the server (see {@code DeathManager}); this class only touches the screen.
 */
public final class UnoReverseSave {

    private UnoReverseSave() {
    }

    public static void trigger(Minecraft client) {
        // Drop the flash, the freakout post-chain, the camera shake, and any blackout.
        NukeFlash.stop();
        FreakoutEffect.stop();
        CameraShake.stop();
        Blackout.stop();
        client.gameRenderer.clearPostEffect();

        // The reverse's own totem flourish, using the save card's model.
        client.gameRenderer.displayItemActivation(new ItemStack(ModItems.UNO_REVERSE_CARD));
    }
}
