package site.deforce.owl_on_a_jump_rope;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Owl_on_a_jump_rope implements ModInitializer {

    public static final String MOD_ID = "owl_on_a_jump_rope";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        ModSounds.initialize();

        OwlNetworking.registerCommon();
        OwlNetworking.registerServer();

        DeathManager.initialize();
        CardSpawner.initialize();

        LOGGER.info("Owl on a jump rope is watching. Do not pick up the card.");
    }
}
