package site.deforce.owl_on_a_jump_rope.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import site.deforce.owl_on_a_jump_rope.DeathManager;
import site.deforce.owl_on_a_jump_rope.ModItems;

/**
 * When a player picks up an Owl card, cancel the normal pickup and start the death sequence instead.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    private int pickupDelay;

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void owl_on_a_jump_rope$onCardPickup(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;

        if (!(self.level() instanceof ServerLevel)) {
            return;
        }
        if (this.pickupDelay > 0) {
            return;
        }
        if (getItem().getItem() != ModItems.OWL_CARD) {
            return;
        }
        if (!(player instanceof ServerPlayer victim)) {
            return;
        }
        if (victim.getInventory().getFreeSlot() == -1) {
            // Inventory full: nowhere to stash the held item, so leave the card on the ground.
            ci.cancel();
            return;
        }

        self.discard();
        DeathManager.startDeath(victim);
        ci.cancel();
    }
}
