package site.deforce.owl_on_a_jump_rope.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import site.deforce.owl_on_a_jump_rope.ModItems;

/**
 * The Owl card cannot be dropped. Without this, pressing Q (or dying) would spit a fresh card entity
 * out to pick up again - an infinite re-trigger loop. All player drops funnel through
 * {@link ServerPlayer#drop(ItemStack, boolean, boolean)}, so cancelling it here for the card closes it.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDropMixin {

    @Inject(
            method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"),
            cancellable = true)
    private void owl$noDropCard(ItemStack stack, boolean throwRandomly, boolean retainOwnership,
                                CallbackInfoReturnable<ItemEntity> cir) {
        if (stack.getItem() == ModItems.OWL_CARD) {
            cir.setReturnValue(null);
        }
    }
}
