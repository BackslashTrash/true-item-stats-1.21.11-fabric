package net.backslashtrash.client.stats;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads enchantment value effects off an item stack.
 *
 * <p>Values always come from the enchantment's own registry data, never from a
 * hardcoded table, so datapack changes and most modded enchantments work for free.
 */
public final class EnchantmentReader {

    /**
     * Seeded on purpose. Most value effects ignore the random entirely, but a few
     * sample from a range -- an unseeded source would make tooltip numbers flicker
     * every frame, since tooltips re-render continuously.
     */
    private static final RandomSource RANDOM = RandomSource.create(0L);

    private EnchantmentReader() {
    }

    /** A conditional bonus we cannot evaluate client-side, reported separately. */
    public record Situational(Component label, double value) {
    }

    /** Unconditional total, plus any conditional bonuses listed individually. */
    public record Result(double total, List<Situational> situational) {
    }

    /**
     * Applies every unconditional value effect of the given type in sequence.
     *
     * <p>Conditional effects carry a loot predicate that needs a ServerLevel to
     * evaluate, which a client mod does not have. Rather than guessing, those are
     * computed against the base value and returned separately so the caller can
     * present them as situational.
     */
    public static Result apply(ItemStack stack, double base,
                               DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> effectType) {
        ItemEnchantments enchantments =
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        double running = base;
        List<Situational> situational = new ArrayList<>();

        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int level = entry.getIntValue();

            for (ConditionalEffect<EnchantmentValueEffect> conditional
                    : holder.value().getEffects(effectType)) {
                if (conditional.requirements().isEmpty()) {
                    running = conditional.effect().process(level, RANDOM, (float) running);
                } else {
                    double branch = conditional.effect().process(level, RANDOM, (float) base);
                    situational.add(new Situational(holder.value().description(), branch));
                }
            }
        }

        return new Result(running, situational);
    }

    /** Convenience wrapper for melee attack damage. */
    public static Result meleeDamage(ItemStack stack, double baseDamage) {
        return apply(stack, baseDamage, EnchantmentEffectComponents.DAMAGE);
    }

    /**
     * Enchantment protection points this stack contributes against one damage type.
     *
     * <p>Processing against a base of zero yields the raw per-level contribution,
     * because vanilla protection effects are additions over a linear value.
     */
    public static double protectionPoints(ItemStack stack, DamageProfile profile) {
        ItemEnchantments enchantments =
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        double total = 0.0;
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int level = entry.getIntValue();

            var profiles = VanillaProtection.profilesFor(holder);
            if (profiles.isEmpty() || !profiles.get().contains(profile)) continue;

            for (ConditionalEffect<EnchantmentValueEffect> conditional
                    : holder.value().getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION)) {
                total += conditional.effect().process(level, RANDOM, 0.0f);
            }
        }
        return total;
    }

    /** Protection enchantments on this stack that we could not classify. */
    public static List<Component> unknownProtections(ItemStack stack) {
        ItemEnchantments enchantments =
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        List<Component> unknown = new ArrayList<>();
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            boolean protects = !holder.value()
                    .getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION).isEmpty();
            if (protects && VanillaProtection.profilesFor(holder).isEmpty()) {
                unknown.add(holder.value().description());
            }
        }
        return unknown;
    }
}