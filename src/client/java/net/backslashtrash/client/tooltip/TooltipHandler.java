package net.backslashtrash.client.tooltip;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.backslashtrash.client.stats.AttributeReader;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Registers the tooltip callback and decides where our lines go.
 *
 * <p>This class knows nothing about damage, armour or enchantments. Adding a new
 * item category means adding a branch to {@link #buildFor} and a builder class --
 * the placement logic below never changes.
 */
public final class TooltipHandler {

    private static final String HINT = "Press Ctrl to show base stats";

    private TooltipHandler() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            boolean ctrl = ctrlHeld();

            List<Component> ours = buildFor(stack, ctrl);
            if (ours == null) return;

            int header = VanillaLines.findHeader(lines);
            if (header < 0) {
                // No vanilla block to replace (rare, but possible on odd items).
                lines.addAll(ours);
            } else if (ctrl) {
                // Keep vanilla's block, put ours above it for comparison.
                lines.addAll(header, ours);
            } else {
                lines.addAll(VanillaLines.removeBlock(lines, header), ours);
            }

            if (!ctrl) {
                lines.add(Lines.hint(HINT));
            }
        });
    }

    /** Null when this stack is neither a weapon nor armour. */
    private static List<Component> buildFor(ItemStack stack, boolean detailed) {
        if (AttributeReader.isWeapon(stack)) {
            return WeaponLines.build(stack);
        }
        EquipmentSlot slot = AttributeReader.armorSlot(stack);
        return slot == null ? null : ArmourLines.build(stack, slot, detailed);
    }

    /**
     * Polled directly rather than cached: tooltips re-render every frame, so this
     * updates live while the player is already hovering an item.
     */
    private static boolean ctrlHeld() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
}