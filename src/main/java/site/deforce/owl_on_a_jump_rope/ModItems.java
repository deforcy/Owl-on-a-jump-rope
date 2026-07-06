package site.deforce.owl_on_a_jump_rope;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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

    public static final Item DECK_OF_CARDS = register(
            "deck_of_cards",
            DeckOfCardsItem::new,
            new Item.Properties().durability(8)
    );

    private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "tools_and_utilities"));

    public static void initialize() {
        // Referencing this class from the entrypoint class-loads the fields above, which is what registers the items.
        CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES).register(output -> {
            output.accept(OWL_CARD);
            output.accept(DECK_OF_CARDS);
        });
    }

    private static Item register(String path, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM, Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, path));
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }
}
