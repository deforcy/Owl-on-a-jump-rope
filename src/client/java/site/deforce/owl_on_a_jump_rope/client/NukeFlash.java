package site.deforce.owl_on_a_jump_rope.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import site.deforce.owl_on_a_jump_rope.Owl_on_a_jump_rope;

/**
 * Full-screen white flash. Drawn in-game (this HUD element) and over open screens (see {@code ScreenMixin}).
 *
 * <p>Two shapes: the doom flash slams to full white and fades out (~3s); the "soft" flash used by the
 * uno-reverse finale eases in and back out, so it can wash over the tail of the reverse animation.
 */
public final class NukeFlash {

    private NukeFlash() {
    }

    private static final Identifier ELEMENT_ID =
            Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "nuke_flash");

    // Doom flash: instant full white, then fade.
    private static final int FLASH_TICKS = 60; // total length (~3s)
    private static final int FULL_TICKS = 12;  // fully opaque white before the fade begins

    // Soft flash: quick fade in to full white, hold, then a slow fade out.
    // SOFT_FADE_IN must match DeathManager.FLASH_FADE_IN_TICKS (the second sting lands on the full-white frame).
    private static final int SOFT_FADE_IN = 4;   // ~0.2s ramp up
    private static final int SOFT_HOLD = 20;     // ~1s held at full white
    private static final int SOFT_FADE_OUT = 44; // ~2.2s ramp back down
    private static final int SOFT_TICKS = SOFT_FADE_IN + SOFT_HOLD + SOFT_FADE_OUT;

    private static int elapsed = -1; // -1 == inactive
    private static boolean soft;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        HudElementRegistry.addLast(ELEMENT_ID,
                (extractor, delta) -> render(extractor, delta.getGameTimeDeltaPartialTick(false)));
    }

    public static void start() {
        elapsed = 0;
        soft = false;
    }

    /** Eases in and back out instead of slamming to white; used to cover the end of the reverse animation. */
    public static void startSoft() {
        elapsed = 0;
        soft = true;
    }

    public static void stop() {
        elapsed = -1;
    }

    private static int duration() {
        return soft ? SOFT_TICKS : FLASH_TICKS;
    }

    private static void tick() {
        if (elapsed < 0) {
            return;
        }
        elapsed++;
        if (elapsed >= duration()) {
            elapsed = -1;
        }
    }

    public static void render(GuiGraphicsExtractor extractor, float partialTick) {
        if (elapsed < 0) {
            return;
        }
        int dur = duration();
        float t = elapsed + partialTick;
        if (t < 0.0F || t >= dur) {
            return;
        }

        float alpha;
        if (soft) {
            if (t < SOFT_FADE_IN) {
                alpha = t / SOFT_FADE_IN;
            } else if (t < SOFT_FADE_IN + SOFT_HOLD) {
                alpha = 1.0F;
            } else {
                alpha = Math.max(0.0F, 1.0F - (t - SOFT_FADE_IN - SOFT_HOLD) / SOFT_FADE_OUT);
            }
        } else if (t < FULL_TICKS) {
            alpha = 1.0F;
        } else {
            alpha = Math.max(0.0F, 1.0F - (t - FULL_TICKS) / (FLASH_TICKS - FULL_TICKS));
        }

        int a = (int) (alpha * 255.0F) & 0xFF;
        int color = (a << 24) | 0x00FFFFFF;
        extractor.fill(0, 0, extractor.guiWidth(), extractor.guiHeight(), color);
    }
}
