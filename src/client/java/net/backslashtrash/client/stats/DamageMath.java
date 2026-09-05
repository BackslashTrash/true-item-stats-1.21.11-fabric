package net.backslashtrash.client.stats;

/**
 * The vanilla damage mitigation formula.
 *
 * <p>Deliberately free of any Minecraft dependency so it can be reasoned about
 * and tested in isolation. Every number the tooltip shows ultimately comes from
 * here, so a bug in this class is invisible in game -- it just produces a
 * plausible wrong percentage.
 */
public final class DamageMath {

    /** Reference hit used when a single representative number is needed. */
    /** Reference hit: a sword blow from another player, which is what these games are. */
    public static final double REFERENCE_HIT = 6.0;;

    /** Enchantment protection is capped at this many points (= 80% reduction). */
    public static final double EPF_CAP = 20.0;

    private DamageMath() {
    }

    /**
     * Fraction of raw damage that survives armor points and enchantment protection.
     *
     * @param armor     total armor points across the set
     * @param toughness total armor toughness across the set
     * @param epf       enchantment protection points applicable to this profile
     * @param damage    incoming raw damage
     */
    public static double survivingFraction(double armor, double toughness, double epf,
                                           double damage, DamageProfile profile) {
        double armorCut = 0.0;
        if (!profile.bypassesArmor()) {
            // 4D/(T+8) is armor penetration: big hits punch through.
            // The A/5 floor guarantees 4% per armor point no matter how big the hit.
            double penetrated = armor - (4.0 * damage) / (toughness + 8.0);
            armorCut = Math.clamp(penetrated, armor / 5.0, EPF_CAP) / 25.0;
        }

        double epfCut = 0.0;
        if (!profile.bypassesEnchantments()) {
            epfCut = Math.min(EPF_CAP, epf) / 25.0;
        }

        return (1.0 - armorCut) * (1.0 - epfCut);
    }

    /** Damage actually taken from a raw hit, after all mitigation. */
    public static double damageTaken(double armor, double toughness, double epf,
                                     double damage, DamageProfile profile) {
        return damage * survivingFraction(armor, toughness, epf, damage, profile);
    }

    /** Reduction as a percentage in the range 0..100. */
    public static double reductionPercent(double armor, double toughness, double epf,
                                          double damage, DamageProfile profile) {
        return (1.0 - survivingFraction(armor, toughness, epf, damage, profile)) * 100.0;
    }

    /**
     * Effective health multiplier -- how many times your real health pool is worth.
     *
     * <p>Use this for comparing gear. Reduction percentage compresses badly at the
     * top end: 90% to 95% halves the damage you take but only reads as five points.
     * As effective health that is x10 to x20, which is legible at a glance.
     */
    public static double effectiveHealth(double armor, double toughness, double epf,
                                         double damage, DamageProfile profile) {
        double surviving = survivingFraction(armor, toughness, epf, damage, profile);
        return surviving <= 0.0 ? Double.POSITIVE_INFINITY : 1.0 / surviving;
    }
}