package site.deforce.owl_on_a_jump_rope;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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
import java.util.function.Consumer;

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

    // ---- Uno-reverse rescue tuning ----------------------------------------
    // How long after pickup a doomed player can still be saved by equipping an uno-reverse card:
    // the full jingle plus the whole bombardment, so "at any moment" really means any moment.
    private static final int SAVE_WINDOW_TICKS = ModSounds.SOVA_LENGTH_TICKS + BOMBARDMENT_TICKS;
    // Gap between the reverse's two stings (respawn-anchor charge -> totem use). Exactly two seconds.
    private static final int SECOND_STING_DELAY = 40;
    // Client soft-flash fade-in length (must match NukeFlash.SOFT_FADE_IN). The flash is launched this
    // many ticks early so it reaches full white exactly as the second sting lands.
    private static final int FLASH_FADE_IN_TICKS = 4;
    // -----------------------------------------------------------------------

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final Random RANDOM = new Random();

    private static final List<Countdown> COUNTDOWNS = new ArrayList<>();
    private static final List<Apocalypse> ACTIVE = new ArrayList<>();
    private static final List<Doomed> DOOMED = new ArrayList<>();
    private static final List<Scheduled> SCHEDULED = new ArrayList<>();

    private static final class Countdown {
        final ServerPlayer victim;
        int ticksLeft;

        Countdown(ServerPlayer victim, int ticksLeft) {
            this.victim = victim;
            this.ticksLeft = ticksLeft;
        }
    }

    /** Watches a doomed player for the whole doom, ready to reverse it the instant they equip a save card. */
    private static final class Doomed {
        final ServerPlayer victim;
        int ticksLeft;

        Doomed(ServerPlayer victim, int ticksLeft) {
            this.victim = victim;
            this.ticksLeft = ticksLeft;
        }
    }

    /** A deferred action fired on a future tick (the reverse stages its flash and second sting this way). */
    private static final class Scheduled {
        final ServerPlayer target;
        int ticksLeft;
        final Consumer<ServerPlayer> action;

        Scheduled(ServerPlayer target, int ticksLeft, Consumer<ServerPlayer> action) {
            this.target = target;
            this.ticksLeft = ticksLeft;
            this.action = action;
        }
    }

    /** A destruction front sweeping along a straight line through the victim. */
    private static final class Apocalypse {
        final ServerLevel level;
        final ServerPlayer victim; // who set it off; used to reverse it if they save themselves
        final Vec3 center;   // line's midpoint: victim's position at the moment of doom
        final double dirX;   // unit heading of the line (victim's facing)
        final double dirZ;
        final double perpX;  // unit perpendicular, spreads carves across the line's width
        final double perpZ;
        final Set<UUID> notified = new HashSet<>();
        int age;

        Apocalypse(ServerLevel level, ServerPlayer victim, Vec3 center, double dirX, double dirZ) {
            this.level = level;
            this.victim = victim;
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
        DOOMED.add(new Doomed(victim, SAVE_WINDOW_TICKS));
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
        // Never weld over a save card: selecting it is how the victim escapes (see the doom watch in tick()).
        if (held.getItem() == ModItems.UNO_REVERSE_CARD) {
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

    private static boolean isHoldingSaveCard(ServerPlayer victim) {
        return victim.getItemInHand(InteractionHand.MAIN_HAND).getItem() == ModItems.UNO_REVERSE_CARD
                || victim.getItemInHand(InteractionHand.OFF_HAND).getItem() == ModItems.UNO_REVERSE_CARD;
    }

    /**
     * Undoes an in-progress doom for a victim who equipped a save card: stops the countdown and any live
     * bombardment, cleans up the welded cards, spends one uno-reverse card, and fires the reverse's
     * "no shaders, no destruction" client cue plus its two stings (charge now, totem use one second later).
     */
    private static void reverseDoom(ServerPlayer victim) {
        for (int i = COUNTDOWNS.size() - 1; i >= 0; i--) {
            if (COUNTDOWNS.get(i).victim == victim) {
                COUNTDOWNS.remove(i);
            }
        }
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            if (ACTIVE.get(i).victim == victim) {
                ACTIVE.remove(i);
            }
        }

        clearCards(victim);
        consumeOneSaveCard(victim);

        // Silence the owl's still-playing jingle for the survivor.
        victim.connection.send(new ClientboundStopSoundPacket(ModSounds.SOVA_ID, SoundSource.MASTER));
        // Cancel the shaders/flash/shake on the client and play the uno-reverse totem animation.
        OwlNetworking.sendSave(victim);
        // First stings immediately: the anchor charge plus the warden's sonic boom.
        playSting(victim, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0F, 0.5F);
        playSting(victim, SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 1.0F);
        // Launch the soft flash early so its fade-in finishes right as the second sting fires...
        schedule(victim, SECOND_STING_DELAY - FLASH_FADE_IN_TICKS, OwlNetworking::sendSoftFlash);
        // ...and the second sting lands on the fully-white frame.
        schedule(victim, SECOND_STING_DELAY, p -> playSting(p, SoundEvents.TOTEM_USE, 1.0F, 1.2F));
    }

    private static void schedule(ServerPlayer target, int ticks, Consumer<ServerPlayer> action) {
        SCHEDULED.add(new Scheduled(target, ticks, action));
    }

    private static void consumeOneSaveCard(ServerPlayer victim) {
        Inventory inv = victim.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == ModItems.UNO_REVERSE_CARD) {
                stack.shrink(1);
                return;
            }
        }
    }

    private static void playSting(ServerPlayer p, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        p.connection.send(new ClientboundSoundPacket(
                Holder.direct(sound), SoundSource.MASTER, p.getX(), p.getY(), p.getZ(), volume, pitch, 0L));
    }

    private static void tick(MinecraftServer server) {
        // Save watch: run before the countdown re-welds the card, so equipping a save card wins the tick.
        for (int i = DOOMED.size() - 1; i >= 0; i--) {
            Doomed d = DOOMED.get(i);
            if (d.victim.isRemoved()) {
                DOOMED.remove(i);
                continue;
            }
            if (isHoldingSaveCard(d.victim)) {
                DOOMED.remove(i);
                reverseDoom(d.victim);
                continue;
            }
            if (--d.ticksLeft <= 0) {
                DOOMED.remove(i);
            }
        }

        // Staged pieces of a completed reverse (the soft flash, then the second sting).
        for (int i = SCHEDULED.size() - 1; i >= 0; i--) {
            Scheduled s = SCHEDULED.get(i);
            if (--s.ticksLeft <= 0) {
                SCHEDULED.remove(i);
                if (!s.target.isRemoved()) {
                    s.action.accept(s.target);
                }
            }
        }

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
                    ACTIVE.add(new Apocalypse(sw, c.victim, c.victim.position(), dirX, dirZ));
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

        // The owl card never sits in a player's inventory except while they're already doomed (it's
        // welded into their hand during the countdown). So a non-doomed player who suddenly holds one
        // must have just taken it - from the mod's creative tab, a /give, whatever - and the curse begins.
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!isDoomed(p) && hasOwlCard(p)) {
                startDeath(p);
            }
        }
    }

    private static boolean isDoomed(ServerPlayer p) {
        for (Doomed d : DOOMED) {
            if (d.victim == p) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOwlCard(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() == ModItems.OWL_CARD) {
                return true;
            }
        }
        return false;
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
