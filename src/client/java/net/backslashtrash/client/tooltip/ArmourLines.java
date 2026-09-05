package net.backslashtrash.client.tooltip;

import net.backslashtrash.client.stats.AttributeReader;
import net.backslashtrash.client.stats.DamageMath;
import net.backslashtrash.client.stats.DamageProfile;
import net.backslashtrash.client.stats.EnchantmentReader;
import net.backslashtrash.client.stats.SetStats;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds the stat lines shown for armour, including comparison against the worn set. */
public final class ArmourLines {

    private ArmourLines() {
    }

    public static List<Component> build(ItemStack stack, EquipmentSlot slot, boolean detailed) {
        List<Component> out = new ArrayList<>();

        double armour = AttributeReader.compute(stack, Attributes.ARMOR, slot, 0.0);
        double toughness = AttributeReader.compute(stack, Attributes.ARMOR_TOUGHNESS, slot, 0.0);
        double knockback = AttributeReader.compute(stack, Attributes.KNOCKBACK_RESISTANCE, slot, 0.0);

        out.add(Lines.stat("Armour", String.format("%.0f", armour),
                Palette.ARMOUR_LABEL, Palette.ARMOUR_VALUE));
        if (toughness > 0) {
            out.add(Lines.stat("Toughness", String.format("%.0f", toughness),
                    Palette.ARMOUR_LABEL, Palette.ARMOUR_VALUE));
        }
        if (knockback > 0) {
            // Knockback resistance combines multiplicatively, so it is shown per
            // piece only and never summed into the set figures below.
            out.add(Lines.stat("Knockback Res", String.format("%.0f%%", knockback * 100),
                    Palette.ARMOUR_LABEL, Palette.ARMOUR_VALUE));
        }

        appendSetComparison(out, stack, slot, detailed);
        return out;
    }

    private static void appendSetComparison(List<Component> out, ItemStack stack,
                                            EquipmentSlot slot, boolean detailed) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) return;

        Map<EquipmentSlot, ItemStack> worn = SetStats.worn(player);
        SetStats withPiece = SetStats.aggregate(SetStats.withSwap(worn, slot, stack));

        double reduction = withPiece.reductionPercent(DamageProfile.MELEE, DamageMath.REFERENCE_HIT);
        double ehp = withPiece.effectiveHealth(DamageProfile.MELEE, DamageMath.REFERENCE_HIT);

        out.add(Lines.stat("With set",
                Lines.percent(reduction) + " (" + Lines.multiplier(ehp) + " health)",
                Palette.ARMOUR_LABEL, Palette.ARMOUR_HEADLINE));

        double delta = SetStats.scorePiece(
                stack, slot, worn, DamageProfile.MELEE, DamageMath.REFERENCE_HIT);
        out.add(Lines.delta("vs equipped", delta, " health"));

        if (!detailed) return;

        // Per-profile breakdown: only rows this piece actually changes, otherwise
        // every armour tooltip carries the same six lines.
        for (DamageProfile profile : DamageProfile.values()) {
            if (profile == DamageProfile.MELEE) continue;
            if (EnchantmentReader.protectionPoints(stack, profile) <= 0) continue;

            double profileReduction =
                    withPiece.reductionPercent(profile, DamageMath.REFERENCE_HIT);
            String value = Lines.percent(profileReduction);
            if (withPiece.isCapped(profile)) {
                value += " (capped, " + String.format("%.0f", withPiece.wastedProtection(profile))
                        + " wasted)";
            }
            out.add(Lines.sub(profile.displayName(), value,
                    Palette.ARMOUR_LABEL, Palette.ARMOUR_VALUE));
        }

        for (Component unknown : EnchantmentReader.unknownProtections(stack)) {
            out.add(Lines.sub(unknown.getString(), "unknown effect",
                    Palette.ARMOUR_LABEL, Palette.NEUTRAL));
        }
    }
}