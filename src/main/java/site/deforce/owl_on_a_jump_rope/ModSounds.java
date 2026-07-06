package site.deforce.owl_on_a_jump_rope;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {

    private ModSounds() {
    }

    public static final Identifier SOVA_ID = Identifier.fromNamespaceAndPath(Owl_on_a_jump_rope.MOD_ID, "sova_na_skakalke");
    public static final SoundEvent SOVA_NA_SKAKALKE = SoundEvent.createVariableRangeEvent(SOVA_ID);

    // Jingle length in server ticks (2.87s). The apocalypse fires as it ends.
    public static final int SOVA_LENGTH_TICKS = 57;

    public static void initialize() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, SOVA_ID, SOVA_NA_SKAKALKE);
    }
}
