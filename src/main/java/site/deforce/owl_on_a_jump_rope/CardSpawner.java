package site.deforce.owl_on_a_jump_rope;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Occasionally spawns a cursed card on the ground near a player, looking like an ordinary dropped item.
 */
public final class CardSpawner {

    private CardSpawner() {
    }

    // 1-in-N chance per player per tick (~once every 3.3 minutes per player).
    private static final int SPAWN_CHANCE = 500;
    private static final double MIN_DIST = 4.0;
    private static final double MAX_DIST = 9.0;

    private static final Random RANDOM = new Random();

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (RANDOM.nextInt(SPAWN_CHANCE) != 0) {
                    continue;
                }
                trySpawnNear(player);
            }
        });
    }

    private static void trySpawnNear(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        if (player.isSpectator() || player.isCreative()) {
            return;
        }

        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        double dist = MIN_DIST + RANDOM.nextDouble() * (MAX_DIST - MIN_DIST);
        int x = (int) Math.round(player.getX() + Math.cos(angle) * dist);
        int z = (int) Math.round(player.getZ() + Math.sin(angle) * dist);
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        Vec3 pos = new Vec3(x + 0.5, y + 0.1, z + 0.5);

        // Skip if a card is already lying around near that spot.
        AABB search = new AABB(pos.x - 6, pos.y - 4, pos.z - 6, pos.x + 6, pos.y + 4, pos.z + 6);
        List<ItemEntity> existing = world.getEntities(EntityTypes.ITEM, search,
                e -> e.getItem().getItem() == ModItems.OWL_CARD);
        if (!existing.isEmpty()) {
            return;
        }

        ItemEntity card = new ItemEntity(world, pos.x, pos.y, pos.z, new ItemStack(ModItems.OWL_CARD));
        card.setDeltaMovement(Vec3.ZERO);
        card.setDefaultPickUpDelay();
        world.addFreshEntity(card);
    }
}
