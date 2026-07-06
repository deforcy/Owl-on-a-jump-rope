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

    // "This server doesn't have the mod, so it won't work here", in several languages.
    private static final String[] WARNING_LINES = {
            "[EN] This server does not have the 'Owl on a jump rope' mod installed. The mod will not work here.",
            "[RU] На этом сервере не установлен мод «Сова на скакалке». Мод здесь работать не будет.",
            "[DE] Auf diesem Server ist die Mod „Owl on a jump rope“ nicht installiert. Die Mod funktioniert hier nicht.",
            "[FR] Ce serveur n'a pas le mod « Owl on a jump rope ». Le mod ne fonctionnera pas ici.",
            "[IT] Questo server non ha la mod \"Owl on a jump rope\". La mod non funzionerà qui.",
            "[ES] Este servidor no tiene el mod «Owl on a jump rope». El mod no funcionará aquí.",
            "[PT] Este servidor não tem o mod \"Owl on a jump rope\". O mod não funcionará aqui.",
            "[PL] Ten serwer nie ma moda „Owl on a jump rope”. Mod nie będzie tu działać.",
            "[UK] На цьому сервері немає мода «Сова на скакалці». Мод тут не працюватиме.",
            "[TR] Bu sunucuda 'Owl on a jump rope' modu yok. Mod burada çalışmayacak.",
            "[ZH] 此服务器未安装「Owl on a jump rope」模组，模组在此无法运行。",
            "[JA] このサーバーには「Owl on a jump rope」MODが導入されていません。ここではMODは動作しません。",
            "[KO] 이 서버에는 'Owl on a jump rope' 모드가 없습니다. 여기서는 모드가 작동하지 않습니다."
    };

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
                Component.literal("=== Owl on a jump rope ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (String line : WARNING_LINES) {
            client.player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.YELLOW));
        }
    }
}
