package site.deforce.owl_on_a_jump_rope;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the death sequence: pick up a card, the card locks into the victim's hand while the jingle
 * plays, then a destruction front sweeps along a line through them.
 *
 * <p>Deliberately entity-free. An earlier version spawned tens of thousands of primed TNT entities,
 * whose block drops tanked the TPS until the bombardment fell behind. Instead, every tick under a
 * hard work budget it carves terrain straight to air and fires drop-free explosions.
 */
public final class DeathManager {

    private DeathManager() {
    }

    // ---- Apocalypse tuning ------------------------------------------------
    private static final double HALF_LENGTH = 300.0;      // half the bombarding line's length
    private static final double HALF_WIDTH = 120.0;       // half the line's width
    private static final double DRAMATIC_RADIUS = 320.0;  // flash + freakout range (also applied on entry)

    // Keep FreakoutEffect.DURATION / CameraShake in rough sync with this so the effect ends with the last blast.
    private static final int BOMBARDMENT_TICKS = 150;     // total run length

    private static final int CARVES_PER_TICK = 24;        // hard per-tick work cap -> stable TPS
    private static final int BOOM_EVERY = 6;              // explode on every Nth carve (raycasting is the pricey bit)
    private static final float EXPLOSION_POWER = 15.0F;
    private static final int CRATER_RADIUS = 5;
    private static final int CRATER_DEPTH = 3;
    // Air-out with client updates but no neighbour/redstone/shape cascade.
    private static final int CARVE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    // -----------------------------------------------------------------------

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final Random RANDOM = new Random();

    private static final List<Countdown> COUNTDOWNS = new ArrayList<>();
    private static final List<Apocalypse> ACTIVE = new ArrayList<>();

    private static final class Countdown {
        final ServerPlayer victim;
        int ticksLeft;

        Countdown(ServerPlayer victim, int ticksLeft) {
            this.victim = victim;
            this.ticksLeft = ticksLeft;
        }
    }

    /** A destruction front sweeping along a straight line through the victim. */
    private static final class Apocalypse {
        final ServerLevel level;
        final Vec3 center;   // line's midpoint: victim's position at the moment of doom
        final double dirX;   // unit heading of the line (victim's facing)
        final double dirZ;
        final double perpX;  // unit perpendicular, spreads carves across the line's width
        final double perpZ;
        final Set<UUID> notified = new HashSet<>();
        int age;

        Apocalypse(ServerLevel level, Vec3 center, double dirX, double dirZ) {
            this.level = level;
            this.center = center;
            this.dirX = dirX;
            this.dirZ = dirZ;
            this.perpX = -dirZ;
            this.perpZ = dirX;
        }
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(DeathManager::tick);
        // Black out (rather than red death tint) players the apocalypse kills.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer sp
                    && source.is(DamageTypeTags.IS_EXPLOSION)
                    && isNearActiveApocalypse(sp)) {
                OwlNetworking.sendBlackout(sp);
            }
        });
    }

    public static void startDeath(ServerPlayer victim) {
        for (Countdown c : COUNTDOWNS) {
            if (c.victim == victim) {
                return; // already doomed
            }
        }

        Level level = victim.level();
        // Bound to the victim so it follows them and everyone nearby hears it.
        level.playSound(null, victim, ModSounds.SOVA_NA_SKAKALKE, SoundSource.MASTER, 5.0F, 1.0F);

        OwlNetworking.sendCardPopup(victim, ModSounds.SOVA_LENGTH_TICKS);
        lockCardInHand(victim);

        COUNTDOWNS.add(new Countdown(victim, ModSounds.SOVA_LENGTH_TICKS));
    }

    /**
     * Welds an un-droppable Owl card into the victim's selected slot, stashing whatever they held in a
     * free slot rather than voiding it. If the inventory is full the card isn't forced in this tick.
     */
    private static void lockCardInHand(ServerPlayer victim) {
        Inventory inv = victim.getInventory();
        int slot = inv.getSelectedSlot();
        ItemStack held = inv.getItem(slot);

        if (held.getItem() == ModItems.OWL_CARD) {
            return;
        }
        if (held.isEmpty()) {
            inv.setItem(slot, new ItemStack(ModItems.OWL_CARD));
            return;
        }

        int free = inv.getFreeSlot();
        if (free == -1) {
            return;
        }
        inv.setItem(free, held);
        inv.setItem(slot, new ItemStack(ModItems.OWL_CARD));
    }

    private static void clearCards(ServerPlayer victim) {
        Inventory inv = victim.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() == ModItems.OWL_CARD) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static void tick(MinecraftServer server) {
        for (int i = COUNTDOWNS.size() - 1; i >= 0; i--) {
            Countdown c = COUNTDOWNS.get(i);
            c.ticksLeft--;

            if (!c.victim.isRemoved() && c.victim.level() instanceof ServerLevel) {
                lockCardInHand(c.victim);
            }

            if (c.ticksLeft <= 0) {
                COUNTDOWNS.remove(i);
                clearCards(c.victim);
                if (c.victim.level() instanceof ServerLevel sw) {
                    // MC yaw: 0 = south (+Z), increasing clockwise toward west.
                    double yawRad = Math.toRadians(c.victim.getYRot());
                    double dirX = -Math.sin(yawRad);
                    double dirZ = Math.cos(yawRad);
                    ACTIVE.add(new Apocalypse(sw, c.victim.position(), dirX, dirZ));
                }
            }
        }

        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Apocalypse a = ACTIVE.get(i);
            runApocalypseTick(a);
            a.age++;
            if (a.age >= BOMBARDMENT_TICKS) {
                ACTIVE.remove(i);
            }
        }
    }

    private static void runApocalypseTick(Apocalypse a) {
        ServerLevel level = a.level;

        double totalLength = HALF_LENGTH * 2.0;
        // The front sweeps from one half-length behind the victim to one ahead, passing through them at the midpoint.
        double frontStart = totalLength * a.age / BOMBARDMENT_TICKS;
        double frontEnd = totalLength * (a.age + 1) / BOMBARDMENT_TICKS;
        double ax = a.center.x - a.dirX * HALF_LENGTH;
        double az = a.center.z - a.dirZ * HALF_LENGTH;

        // Flash + curse everyone in range once, including players who only now wandered in.
        for (ServerPlayer p : level.players()) {
            double d = horizontalDistance(p.getX(), p.getZ(), a.center.x, a.center.z);
            if (d <= DRAMATIC_RADIUS && a.notified.add(p.getUUID())) {
                OwlNetworking.sendFlash(p);
                playChargeSound(p);
                strikeLightning(level, p.getX(), p.getY(), p.getZ());
            }
        }

        // Carve + boom the slice of the line the front is sweeping, spread randomly across its width.
        for (int i = 0; i < CARVES_PER_TICK; i++) {
            double along = frontStart + RANDOM.nextDouble() * (frontEnd - frontStart);
            double lateral = (RANDOM.nextDouble() * 2.0 - 1.0) * HALF_WIDTH;
            double x = ax + a.dirX * along + a.perpX * lateral;
            double z = az + a.dirZ * along + a.perpZ * lateral;
            int ix = Mth.floor(x);
            int iz = Mth.floor(z);
            int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, ix, iz);

            carveCrater(level, ix, gy, iz);
            if (i % BOOM_EVERY == 0) {
                explodeNoDrops(level, x, gy, z, EXPLOSION_POWER);
            }
        }

        if (a.age % 8 == 0) {
            double lateral = (RANDOM.nextDouble() * 2.0 - 1.0) * HALF_WIDTH;
            double x = ax + a.dirX * frontEnd + a.perpX * lateral;
            double z = az + a.dirZ * frontEnd + a.perpZ * lateral;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            strikeLightning(level, x, y, z);
        }
    }

    private static void explodeNoDrops(ServerLevel level, double x, double y, double z, float power) {
        level.explode(null, x, y, z, power, true, Level.ExplosionInteraction.NONE);
    }

    private static void carveCrater(ServerLevel level, int cx, int topY, int cz) {
        int minY = level.getMinY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r2 = CRATER_RADIUS * CRATER_RADIUS;

        for (int dx = -CRATER_RADIUS; dx <= CRATER_RADIUS; dx++) {
            for (int dz = -CRATER_RADIUS; dz <= CRATER_RADIUS; dz++) {
                if (dx * dx + dz * dz > r2) {
                    continue;
                }
                for (int dy = 2; dy >= -CRATER_DEPTH; dy--) {
                    int y = topY + dy;
                    if (y <= minY) {
                        break;
                    }
                    pos.set(cx + dx, y, cz + dz);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, AIR, CARVE_FLAGS);
                    }
                }
            }
        }
    }

    /** Plays the respawn-anchor "charge" sting to a single player, centred on them. */
    private static void playChargeSound(ServerPlayer p) {
        p.connection.send(new ClientboundSoundPacket(
                Holder.direct(SoundEvents.RESPAWN_ANCHOR_CHARGE), SoundSource.MASTER,
                p.getX(), p.getY(), p.getZ(), 1.0F, 0.1F, 0L));
    }

    private static boolean isNearActiveApocalypse(ServerPlayer p) {
        for (Apocalypse a : ACTIVE) {
            if (a.level == p.level()
                    && horizontalDistance(p.getX(), p.getZ(), a.center.x, a.center.z) <= DRAMATIC_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static double horizontalDistance(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void strikeLightning(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.snapTo(x, y, z);
            level.addFreshEntity(bolt);
        }
    }
}
