package site.deforce.owl_on_a_jump_rope.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import site.deforce.owl_on_a_jump_rope.Owl_on_a_jump_rope;

/**
 * Full-screen white flash that fades over ~3s. Drawn in-game (this HUD element) and over open
 * screens (see {@code ScreenMixin}).
 */
public final class NukeFlash {

    private NukeFlash() {
    }

    private static final Identifier ELEMENT_ID =
            Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "nuke_flash");

    private static final int FLASH_TICKS = 60; // total length (~3s)
    private static final int FULL_TICKS = 12;  // fully opaque white before the fade begins

    private static int elapsed = -1; // -1 == inactive

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        HudElementRegistry.addLast(ELEMENT_ID,
                (extractor, delta) -> render(extractor, delta.getGameTimeDeltaPartialTick(false)));
    }

    public static void start() {
        elapsed = 0;
    }

    private static void tick() {
        if (elapsed < 0) {
            return;
        }
        elapsed++;
        if (elapsed >= FLASH_TICKS) {
            elapsed = -1;
        }
    }

    public static void render(GuiGraphicsExtractor extractor, float partialTick) {
        if (elapsed < 0) {
            return;
        }
        float t = elapsed + partialTick;
        if (t < 0.0F || t >= FLASH_TICKS) {
            return;
        }

        float alpha;
        if (t < FULL_TICKS) {
            alpha = 1.0F;
        } else {
            alpha = Math.max(0.0F, 1.0F - (t - FULL_TICKS) / (FLASH_TICKS - FULL_TICKS));
        }

        int a = (int) (alpha * 255.0F) & 0xFF;
        int color = (a << 24) | 0x00FFFFFF;
        extractor.fill(0, 0, extractor.guiWidth(), extractor.guiHeight(), color);
    }
}
