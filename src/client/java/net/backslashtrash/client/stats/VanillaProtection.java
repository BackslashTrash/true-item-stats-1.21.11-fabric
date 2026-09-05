package net.backslashtrash.client.stats;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Which damage types each vanilla protection enchantment applies to.
 *
 * <p>Only the classification is hardcoded here. The actual per-level value is
 * always read from the enchantment's own data, so a datapack that retunes
 * Protection flows through correctly.
 *
 * <p>An empty Optional means "unknown", which is different from an empty set
 * meaning "applies to nothing". Unknown enchantments must be surfaced to the
 * player rather than silently counted as zero.
 */
public final class VanillaProtection {

    /** Protection covers everything except sonic boom (and hunger/void/kill, which we don't model). */
    private static final Set<DamageProfile> GENERAL =
            EnumSet.complementOf(EnumSet.of(DamageProfile.SONIC_BOOM));

    private VanillaProtection() {
    }

    public static Optional<Set<DamageProfile>> profilesFor(Holder<Enchantment> enchantment) {
        // Holder.is(ResourceKey) is an exact registry match -- no string handling,
        // and a typo in the constant name fails at compile time rather than
        // silently classifying an enchantment as unknown.
        if (enchantment.is(Enchantments.PROTECTION)) {
            return Optional.of(GENERAL);
        }
        if (enchantment.is(Enchantments.FIRE_PROTECTION)) {
            return Optional.of(EnumSet.of(DamageProfile.FIRE));
        }
        if (enchantment.is(Enchantments.BLAST_PROTECTION)) {
            return Optional.of(EnumSet.of(DamageProfile.EXPLOSION));
        }
        if (enchantment.is(Enchantments.PROJECTILE_PROTECTION)) {
            return Optional.of(EnumSet.of(DamageProfile.PROJECTILE));
        }
        if (enchantment.is(Enchantments.FEATHER_FALLING)) {
            return Optional.of(EnumSet.of(DamageProfile.FALL));
        }
        return Optional.empty();
    }
    /** True if this stack carries an enchantment specific to this damage type. */
    public static boolean specialisesIn(ItemStack stack, DamageProfile profile) {
        if (profile == DamageProfile.MELEE) return false;
        ItemEnchantments enchants =
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var e : enchants.entrySet()) {
            var profiles = profilesFor(e.getKey());
            // A specific protection maps to exactly one profile; Protection maps to many.
            if (profiles.isPresent() && profiles.get().size() == 1
                    && profiles.get().contains(profile)) {
                return true;
            }
        }
        return false;
    }
}