package site.deforce.owl_on_a_jump_rope.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the private {@code GameRenderer.setPostEffect(Identifier)} so we can force our own chain on. */
@Mixin(GameRenderer.class)
public interface GameRendererInvoker {

    @Invoker("setPostEffect")
    void owl$setPostEffect(Identifier id);
}
