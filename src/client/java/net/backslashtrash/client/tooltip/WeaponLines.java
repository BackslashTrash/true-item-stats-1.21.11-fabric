package net.backslashtrash.client.tooltip;

import net.backslashtrash.client.stats.AttributeReader;
import net.backslashtrash.client.stats.EnchantmentReader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Builds the stat lines shown for melee weapons. */
public final class WeaponLines {

    /** Item modifiers add to the player's own base values, they do not replace them. */
    private static final double PLAYER_BASE_DAMAGE = 1.0;
    private static final double PLAYER_BASE_SPEED = 4.0;

    private WeaponLines() {
    }

    public static List<Component> build(ItemStack stack) {
        double baseDamage = AttributeReader.compute(
                stack, Attributes.ATTACK_DAMAGE, EquipmentSlot.MAINHAND, PLAYER_BASE_DAMAGE);
        double speed = AttributeReader.compute(
                stack, Attributes.ATTACK_SPEED, EquipmentSlot.MAINHAND, PLAYER_BASE_SPEED);

        EnchantmentReader.Result result = EnchantmentReader.meleeDamage(stack, baseDamage);
        double damage = result.total();

        List<Component> out = new ArrayList<>();
        out.add(Lines.stat("Damage", String.format("%.1f", damage),
                Palette.WEAPON_LABEL, Palette.WEAPON_DAMAGE));
        out.add(Lines.stat("Speed", String.format("%.2f", speed),
                Palette.WEAPON_LABEL, Palette.WEAPON_SPEED));
        out.add(Lines.stat("DPS", String.format("%.1f", damage * speed),
                Palette.WEAPON_LABEL, Palette.WEAPON_DPS));

        // Conditional enchantments (Smite, Bane of Arthropods) cannot be folded
        // into the headline number without lying about when they apply.
        for (EnchantmentReader.Situational s : result.situational()) {
            out.add(Lines.sub(s.label().getString(), String.format("%.1f", s.value()),
                    Palette.WEAPON_LABEL, Palette.WEAPON_DAMAGE));
        }
        return out;
    }
}