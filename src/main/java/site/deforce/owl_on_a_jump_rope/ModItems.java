package site.deforce.owl_on_a_jump_rope;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import site.deforce.owl_on_a_jump_rope.item.DeckOfCardsItem;

import java.util.function.Function;

public final class ModItems {

    private ModItems() {
    }

    public static final Item OWL_CARD = register(
            "owl_card",
            Item::new,
            new Item.Properties().stacksTo(16)
    );

    /** "Save card": holding one while doomed reverses the whole death sequence. See {@code DeathManager}. */
    public static final Item UNO_REVERSE_CARD = register(
            "uno_reverse_card",
            Item::new,
            new Item.Properties().stacksTo(4)
    );

    public static final Item DECK_OF_CARDS = register(
            "deck_of_cards",
            DeckOfCardsItem::new,
            new Item.Properties().durability(8)
    );

    /** The mod's own creative tab; every card lives here rather than in a vanilla group. */
    public static final CreativeModeTab OWL_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "owl_cards"),
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(OWL_CARD))
                    .title(Component.translatable("itemGroup.owl_on_a_jump_rope.owl_cards"))
                    .displayItems((params, output) -> {
                        output.accept(OWL_CARD);
                        output.accept(UNO_REVERSE_CARD);
                        output.accept(DECK_OF_CARDS);
                    })
                    .build());

    public static void initialize() {
        // Referencing this class from the entrypoint class-loads the fields above, which registers the
        // items and the creative tab.
    }

    private static Item register(String path, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM, Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, path));
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }
}
