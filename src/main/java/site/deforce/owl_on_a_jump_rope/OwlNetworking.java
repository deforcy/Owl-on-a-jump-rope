package site.deforce.owl_on_a_jump_rope;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class OwlNetworking {

    private OwlNetworking() {
    }

    /** Handshake channel used only so a client can detect whether the server has the mod. */
    public record HandshakePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HandshakePayload> TYPE =
                new CustomPacketPayload.Type<>(
                        Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "handshake"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HandshakePayload> CODEC =
                StreamCodec.unit(new HandshakePayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Sent to a victim on pickup; carries how long the card animation should last. */
    public record CardPopupPayload(int durationTicks) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CardPopupPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "card_popup"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CardPopupPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, CardPopupPayload::durationTicks,
                        CardPopupPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Sent when the song ends: the receiving client flashes blinding white. */
    public record FlashPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<FlashPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "flash"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FlashPayload> CODEC =
                StreamCodec.unit(new FlashPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Sent to a player killed by the apocalypse: their screen goes fully black instead of the red death tint. */
    public record BlackoutPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BlackoutPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "blackout"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BlackoutPayload> CODEC =
                StreamCodec.unit(new BlackoutPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Sent when an uno-reverse card saves a doomed player: the client undoes the doom visuals. */
    public record SavePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SavePayload> TYPE =
                new CustomPacketPayload.Type<>(
                        Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "save"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SavePayload> CODEC =
                StreamCodec.unit(new SavePayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Sent with the reverse's second sting: the client eases a soft white flash over the animation's tail. */
    public record SoftFlashPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SoftFlashPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "soft_flash"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SoftFlashPayload> CODEC =
                StreamCodec.unit(new SoftFlashPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CardPopupPayload.TYPE, CardPopupPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FlashPayload.TYPE, FlashPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BlackoutPayload.TYPE, BlackoutPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SavePayload.TYPE, SavePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SoftFlashPayload.TYPE, SoftFlashPayload.CODEC);
    }

    public static void sendCardPopup(ServerPlayer victim, int durationTicks) {
        ServerPlayNetworking.send(victim, new CardPopupPayload(durationTicks));
    }

    public static void sendFlash(ServerPlayer player) {
        ServerPlayNetworking.send(player, new FlashPayload());
    }

    public static void sendBlackout(ServerPlayer player) {
        ServerPlayNetworking.send(player, new BlackoutPayload());
    }

    public static void sendSave(ServerPlayer player) {
        ServerPlayNetworking.send(player, new SavePayload());
    }

    public static void sendSoftFlash(ServerPlayer player) {
        ServerPlayNetworking.send(player, new SoftFlashPayload());
    }

    public static void registerServer() {
        // The receiver does nothing (the client never sends this); its mere presence is what makes
        // the server advertise the channel, which is how clients detect the mod is installed.
        ServerPlayNetworking.registerGlobalReceiver(HandshakePayload.TYPE, (payload, context) -> {
        });
    }
}
