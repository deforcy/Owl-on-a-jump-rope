package site.deforce.owl_on_a_jump_rope.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import site.deforce.owl_on_a_jump_rope.Owl_on_a_jump_rope;

/**
 * When the apocalypse kills a player, holds their screen solid black and silent until they respawn.
 */
public final class Blackout {

    private Blackout() {
    }

    private static final Identifier ELEMENT_ID =
            Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "blackout");

    // Failsafe so a weird state can never trap someone in silent black forever (10 real minutes).
    private static final int MAX_TICKS = 12000;

    private static boolean active;
    private static boolean sawDead; // confirmed dead before we start watching for respawn
    private static int ticks;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(Blackout::tick);
        HudElementRegistry.addLast(ELEMENT_ID, (extractor, delta) -> render(extractor));
    }

    public static void start() {
        active = true;
        sawDead = false;
        ticks = 0;
    }

    public static void stop() {
        active = false;
    }

    public static boolean active() {
        return active;
    }

    private static void tick(Minecraft client) {
        if (!active) {
            return;
        }
        ticks++;

        LocalPlayer player = client.player;
        boolean dead = player != null && player.isDeadOrDying();
        if (dead) {
            sawDead = true;
        }
        // Lift once the player has died and come back to life, or after the failsafe.
        if ((sawDead && player != null && !dead) || ticks > MAX_TICKS) {
            active = false;
            return;
        }

        client.getSoundManager().stop();
    }

    public static void render(GuiGraphicsExtractor extractor) {
        if (!active) {
            return;
        }
        extractor.fill(0, 0, extractor.guiWidth(), extractor.guiHeight(), 0xFF000000);
    }
}
