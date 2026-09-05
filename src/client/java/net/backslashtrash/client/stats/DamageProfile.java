package net.backslashtrash.client.stats;

/**
 * Categories of incoming damage the mod reports on.
 *
 * <p>The flags mirror vanilla's damage type tags: some damage sources skip the
 * armor points calculation entirely, and a few skip enchantment protection too.
 */
public enum DamageProfile {
    MELEE("Melee", false, false),
    PROJECTILE("Projectile", false, false),
    EXPLOSION("Explosion", false, false),
    FIRE("Fire", false, false),
    /** Fall damage ignores armor points, which is why Feather Falling exists. */
    FALL("Fall", true, false),
    /** Warden sonic boom ignores armor and protection alike. */
    SONIC_BOOM("Sonic Boom", true, true);

    private final String displayName;
    private final boolean bypassesArmor;
    private final boolean bypassesEnchantments;

    DamageProfile(String displayName, boolean bypassesArmor, boolean bypassesEnchantments) {
        this.displayName = displayName;
        this.bypassesArmor = bypassesArmor;
        this.bypassesEnchantments = bypassesEnchantments;
    }

    public String displayName() {
        return displayName;
    }

    public boolean bypassesArmor() {
        return bypassesArmor;
    }

    public boolean bypassesEnchantments() {
        return bypassesEnchantments;
    }
}