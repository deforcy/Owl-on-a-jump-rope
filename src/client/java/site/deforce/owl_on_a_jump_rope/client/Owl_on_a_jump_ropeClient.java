package site.deforce.owl_on_a_jump_rope.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import site.deforce.owl_on_a_jump_rope.ModItems;
import site.deforce.owl_on_a_jump_rope.OwlNetworking;

public class Owl_on_a_jump_ropeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Play the vanilla totem-of-undying activation animation with the cursed card.
        ClientPlayNetworking.registerGlobalReceiver(OwlNetworking.CardPopupPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> client.gameRenderer.displayItemActivation(new ItemStack(ModItems.OWL_CARD)));
        });

        NukeFlash.register();
        FreakoutEffect.register();
        CameraShake.register();
        Blackout.register();
        ClientPlayNetworking.registerGlobalReceiver(OwlNetworking.FlashPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    NukeFlash.start();
                    FreakoutEffect.start();
                }));

        ClientPlayNetworking.registerGlobalReceiver(OwlNetworking.BlackoutPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                Blackout.start();
                client.getSoundManager().stop();
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // No handshake receiver on the server means it doesn't have the mod.
            if (!ClientPlayNetworking.canSend(OwlNetworking.HandshakePayload.TYPE)) {
                client.execute(() -> notifyMissingServerMod(client));
            }
        });
    }

    private static void notifyMissingServerMod(Minecraft client) {
        if (client.player == null) {
            return;
        }
        client.player.sendSystemMessage(
                Component.translatable("message.owl_on_a_jump_rope.missing_server_mod")
                        .withStyle(ChatFormatting.YELLOW));
    }
}
