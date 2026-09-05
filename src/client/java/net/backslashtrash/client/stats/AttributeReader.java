package net.backslashtrash.client.stats;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Reads attribute values off an item stack's {@code attribute_modifiers} component.
 *
 * <p>Reading the component rather than a hardcoded material table means armor
 * trims, modded gear and datapack-customised items all work with no extra code.
 */
public final class AttributeReader {

    /** Slots that can carry armor, in the order we probe them. */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.BODY
    };

    private AttributeReader() {
    }

    /**
     * Total value of one attribute for one slot, applying all three modifier
     * operations in vanilla's order.
     *
     * @param base the wearer's own base value for this attribute (0 for armor,
     *             1.0 for attack damage, 4.0 for attack speed)
     */
    public static double compute(ItemStack stack, Holder<Attribute> attribute,
                                 EquipmentSlot slot, double base) {
        ItemAttributeModifiers modifiers =
                stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        double flat = 0.0;
        double percentOfBase = 0.0;
        double percentOfTotal = 1.0;

        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!entry.attribute().equals(attribute)) continue;
            // slot() is an EquipmentSlotGroup predicate, not a single slot --
            // a modifier tagged ANY or HAND must still count for MAINHAND.
            if (!entry.slot().test(slot)) continue;

            AttributeModifier mod = entry.modifier();
            switch (mod.operation()) {
                case ADD_VALUE -> flat += mod.amount();
                case ADD_MULTIPLIED_BASE -> percentOfBase += mod.amount();
                case ADD_MULTIPLIED_TOTAL -> percentOfTotal *= (1.0 + mod.amount());
            }
        }

        return (base + flat) * (1.0 + percentOfBase) * percentOfTotal;
    }

    /** Whether this stack carries any modifier for the given attribute and slot. */
    public static boolean has(ItemStack stack, Holder<Attribute> attribute, EquipmentSlot slot) {
        ItemAttributeModifiers modifiers =
                stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(attribute) && entry.slot().test(slot)) {
                return true;
            }
        }
        return false;
    }

    /** True if this stack is usable as a melee weapon. */
    public static boolean isWeapon(ItemStack stack) {
        return has(stack, Attributes.ATTACK_DAMAGE, EquipmentSlot.MAINHAND);
    }

    /**
     * The slot this armor piece belongs in, or null if it is not armor.
     *
     * <p>Determined by which slot actually carries an armor modifier, rather than
     * by the equippable component, so unusual and modded gear resolves correctly.
     */
    public static EquipmentSlot armorSlot(ItemStack stack) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (has(stack, Attributes.ARMOR, slot)) return slot;
        }
        return null;
    }
}