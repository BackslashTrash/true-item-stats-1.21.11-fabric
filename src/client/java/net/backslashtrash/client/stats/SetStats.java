package net.backslashtrash.client.stats;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Aggregated defensive stats for a whole armor set.
 *
 * <p>Armor points are set-wide and enchantment protection is a shared capped pool,
 * so a single piece's worth genuinely depends on the other three. Nothing here
 * scores a piece in isolation.
 *
 * <p>Knockback resistance is deliberately absent: it combines multiplicatively in
 * vanilla, not additively, so summing it across a set would be wrong. Display it
 * per piece instead.
 */
public record SetStats(double armor, double toughness, Map<DamageProfile, Double> protection) {

    public double protectionFor(DamageProfile profile) {
        return protection.getOrDefault(profile, 0.0);
    }

    public boolean isCapped(DamageProfile profile) {
        return protectionFor(profile) >= DamageMath.EPF_CAP;
    }

    /** Protection points beyond the cap, which do nothing. */
    public double wastedProtection(DamageProfile profile) {
        return Math.max(0.0, protectionFor(profile) - DamageMath.EPF_CAP);
    }

    /** The armor the player is currently wearing, keyed by slot. */
    public static Map<EquipmentSlot, ItemStack> worn(Player player) {
        Map<EquipmentSlot, ItemStack> map = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            map.put(slot, player.getItemBySlot(slot));
        }
        return map;
    }

    public static SetStats aggregate(Map<EquipmentSlot, ItemStack> worn) {
        double armor = 0.0;
        double toughness = 0.0;
        Map<DamageProfile, Double> protection = new EnumMap<>(DamageProfile.class);

        for (Map.Entry<EquipmentSlot, ItemStack> e : worn.entrySet()) {
            ItemStack stack = e.getValue();
            if (stack == null || stack.isEmpty()) continue;
            EquipmentSlot slot = e.getKey();

            armor += AttributeReader.compute(stack, Attributes.ARMOR, slot, 0.0);
            toughness += AttributeReader.compute(stack, Attributes.ARMOR_TOUGHNESS, slot, 0.0);

            for (DamageProfile profile : DamageProfile.values()) {
                protection.merge(profile,
                        EnchantmentReader.protectionPoints(stack, profile), Double::sum);
            }
        }
        return new SetStats(armor, toughness, protection);
    }

    /** The same set with one slot swapped for a candidate piece. */
    public static Map<EquipmentSlot, ItemStack> withSwap(Map<EquipmentSlot, ItemStack> worn,
                                                         EquipmentSlot slot, ItemStack candidate) {
        Map<EquipmentSlot, ItemStack> swapped = new EnumMap<>(worn);
        swapped.put(slot, candidate);
        return swapped;
    }

    public double effectiveHealth(DamageProfile profile, double damage) {
        return DamageMath.effectiveHealth(armor, toughness, protectionFor(profile), damage, profile);
    }

    public double reductionPercent(DamageProfile profile, double damage) {
        return DamageMath.reductionPercent(armor, toughness, protectionFor(profile), damage, profile);
    }

    /**
     * Change in effective health from equipping a candidate piece. Positive is an
     * upgrade.
     *
     * <p>This is the only correct way to value a piece. A Protection IV item scores
     * exactly zero when the other three slots already cap the pool -- an answer no
     * per-piece formula can produce.
     */
    public static double scorePiece(ItemStack candidate, EquipmentSlot slot,
                                    Map<EquipmentSlot, ItemStack> worn,
                                    DamageProfile profile, double damage) {
        double after = aggregate(withSwap(worn, slot, candidate)).effectiveHealth(profile, damage);
        double before = aggregate(worn).effectiveHealth(profile, damage);
        return after - before;
    }
}