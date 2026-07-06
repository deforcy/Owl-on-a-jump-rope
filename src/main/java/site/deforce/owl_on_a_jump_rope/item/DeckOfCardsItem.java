package site.deforce.owl_on_a_jump_rope.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import site.deforce.owl_on_a_jump_rope.ModItems;

/**
 * Every use tosses a cursed card out in front of the player; picking it up starts the death sequence
 * (see {@code ItemEntityMixin}). Breaks after 8 uses.
 */
public class DeckOfCardsItem extends Item {

    public DeckOfCardsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 look = user.getViewVector(1.0F);
            Vec3 spawnPos = user.getEyePosition().add(look.scale(0.6)).subtract(0.0, 0.2, 0.0);

            ItemEntity card = new ItemEntity(
                    serverLevel,
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    new ItemStack(ModItems.OWL_CARD)
            );
            card.setDeltaMovement(look.x * 0.3, look.y * 0.3 + 0.1, look.z * 0.3);
            card.setPickUpDelay(10);
            serverLevel.addFreshEntity(card);

            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 0.7F);

            user.getItemInHand(hand).hurtAndBreak(1, user, hand);
        }

        return InteractionResult.SUCCESS;
    }
}
