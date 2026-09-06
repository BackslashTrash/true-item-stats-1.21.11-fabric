package net.backslashtrash.client.tooltip;

import net.backslashtrash.client.stats.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds the stat lines shown for armour, including comparison against the worn set. */
public final class ArmourLines {

    /** How far a profile must diverge from the melee figure to earn a row, in points. */
    private static final double NOTABLE_DIFFERENCE = 0.01;

    private ArmourLines() {
    }

    public static List<Component> build(ItemStack stack, EquipmentSlot slot, boolean detailed) {
        List<Component> out = new ArrayList<>();
        appendSetComparison(out, stack, slot, detailed);

        if (detailed) {
            appendRawAttributes(out, stack, slot);
        }
        return out;
    }

    private static void appendSetComparison(List<Component> out, ItemStack stack,
                                            EquipmentSlot slot, boolean detailed) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) return;

        Map<EquipmentSlot, ItemStack> worn = SetStats.worn(player);
        ItemStack current = worn.get(slot);

        // Aggregate the unchanged set once and reuse it as the comparison baseline,
        // rather than re-aggregating inside every per-profile calculation.
        SetStats baseline = SetStats.aggregate(worn);
        SetStats withPiece = SetStats.aggregate(SetStats.withSwap(worn, slot, stack));

        double reduction = withPiece.reductionPercent(DamageProfile.MELEE, DamageMath.REFERENCE_HIT);
        double ehp = withPiece.effectiveHealth(DamageProfile.MELEE, DamageMath.REFERENCE_HIT);

        out.add(Lines.stat("With set",
                Lines.percent(reduction) + " (" + Lines.multiplier(ehp) + " health)",
                Palette.ARMOUR_LABEL, Palette.ARMOUR_HEADLINE));
        out.add(Lines.deltaPercent("vs equipped",
                relativeChange(withPiece, baseline, DamageProfile.MELEE)));

        Set<DamageProfile> shown = alwaysShown(stack, current);
        for (DamageProfile profile : shown) {
            appendProfile(out, profile, withPiece, baseline, reduction, true);
        }

        // Unclassified protection enchantments are surfaced always, never hidden
        // behind Ctrl -- silently omitting one is worse than admitting uncertainty.
        for (Component unknown : EnchantmentReader.unknownProtections(stack)) {
            out.add(Lines.sub(unknown.getString(), "unknown effect",
                    Palette.ARMOUR_LABEL, Palette.NEUTRAL));
        }

        if (!detailed) return;

        for (DamageProfile profile : DamageProfile.values()) {
            if (profile == DamageProfile.MELEE || shown.contains(profile)) continue;
            appendProfile(out, profile, withPiece, baseline, reduction, false);
        }
    }

    /**
     * Profiles that get a row without pressing Ctrl.
     *
     * <p>Includes what the CURRENT piece specialises in, not only the hovered one.
     * Swapping Blast Protection for Projectile Protection is a trade, and a tooltip
     * that shows only the gain is telling half the story.
     */
    private static Set<DamageProfile> alwaysShown(ItemStack candidate, ItemStack current) {
        // Bows are everywhere in the games this is built for, so projectile always shows.
        Set<DamageProfile> profiles = EnumSet.of(DamageProfile.PROJECTILE);

        for (DamageProfile profile : DamageProfile.values()) {
            if (profile == DamageProfile.MELEE) continue;
            if (VanillaProtection.specialisesIn(candidate, profile)) profiles.add(profile);
            if (current != null && VanillaProtection.specialisesIn(current, profile)) {
                profiles.add(profile);
            }
        }
        return profiles;
    }

    /**
     * One row per damage type: the reduction, and what swapping does to it.
     *
     * <p>The whole value is coloured by the direction of its own change, so a trade
     * reads at a glance -- green on the type you gain, red on the type you give up.
     *
     * @param force show the row even when nothing about it is notable
     */
    private static void appendProfile(List<Component> out, DamageProfile profile,
                                      SetStats withPiece, SetStats baseline,
                                      double meleeReduction, boolean force) {
        double reduction = withPiece.reductionPercent(profile, DamageMath.REFERENCE_HIT);
        double change = relativeChange(withPiece, baseline, profile);

        boolean differsFromMelee = Math.abs(reduction - meleeReduction) >= NOTABLE_DIFFERENCE;
        boolean moves = Math.abs(change) >= Lines.EPSILON;
        if (!force && !differsFromMelee && !moves) return;

        StringBuilder value = new StringBuilder(Lines.percent(reduction));
        if (moves) {
            value.append(" (").append(Lines.signedPercent(change)).append(")");
        }
        if (withPiece.isCapped(profile)) {
            value.append(" (capped)");
        }

        out.add(Lines.sub(profile.displayName(), value.toString(),
                Palette.ARMOUR_LABEL, Lines.colourFor(change)));
    }

    /**
     * Percentage change in effective health from making this swap.
     *
     * <p>Relative, not absolute: effective health compounds, so the same piece is
     * worth far more raw effective health as a fourth item than as a first. The
     * ratio is scale-free and means the same thing at any armour level.
     */
    private static double relativeChange(SetStats withPiece, SetStats baseline,
                                         DamageProfile profile) {
        double before = baseline.effectiveHealth(profile, DamageMath.REFERENCE_HIT);
        double after = withPiece.effectiveHealth(profile, DamageMath.REFERENCE_HIT);
        if (before <= 0 || Double.isInfinite(before)) return 0.0;
        return (after / before - 1.0) * 100.0;
    }

    /** Raw inputs to the numbers above. Behind Ctrl, since the numbers are the point. */
    private static void appendRawAttributes(List<Component> out, ItemStack stack, EquipmentSlot slot) {
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
            // Knockback resistance combines multiplicatively across pieces, so it is
            // shown per piece only and never summed into the set figures.
            out.add(Lines.stat("Knockback Res", String.format("%.0f%%", knockback * 100),
                    Palette.ARMOUR_LABEL, Palette.ARMOUR_VALUE));
        }
    }
}