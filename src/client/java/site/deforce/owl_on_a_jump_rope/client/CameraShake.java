package site.deforce.owl_on_a_jump_rope.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.Random;

/**
 * Hands out random per-frame jitters that {@code CameraMixin} applies to the camera while active.
 */
public final class CameraShake {

    private CameraShake() {
    }

    private static final Random RANDOM = new Random();

    /** Peak angular jitter, in degrees. */
    private static final float ROT_INTENSITY = 1.6F;
    /** Peak positional jitter, in blocks. */
    private static final float MOVE_INTENSITY = 0.06F;

    private static int ticksLeft = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ticksLeft > 0) {
                ticksLeft--;
            }
        });
    }

    public static void start(int durationTicks) {
        ticksLeft = durationTicks;
    }

    public static boolean active() {
        return ticksLeft > 0;
    }

    private static float jitter(float intensity) {
        return (RANDOM.nextFloat() * 2.0F - 1.0F) * intensity;
    }

    public static float yawJitter() {
        return jitter(ROT_INTENSITY);
    }

    public static float pitchJitter() {
        return jitter(ROT_INTENSITY);
    }

    public static float moveJitter() {
        return jitter(MOVE_INTENSITY);
    }
}
