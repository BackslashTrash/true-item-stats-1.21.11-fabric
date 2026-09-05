package net.backslashtrash.client.tooltip;

/**
 * Colours, named by role rather than by position in a palette.
 *
 * <p>Naming these WEAPON_DAMAGE rather than COLOUR1 means "make the damage number
 * brighter" is a one-line change instead of a grep through every call site.
 */
public final class Palette {

    private Palette() {
    }

    // Weapons -- green/teal
    public static final int WEAPON_LABEL = 0x555555;
    public static final int WEAPON_DAMAGE = 0xBCE784;
    public static final int WEAPON_SPEED = 0x5DD39E;
    public static final int WEAPON_DPS = 0x348AA7;

    // Armour -- lavender/blue
    public static final int ARMOUR_LABEL = 0x555555;
    public static final int ARMOUR_VALUE = 0xC2BBF0;
    public static final int ARMOUR_HEADLINE = 0x8FB8ED;

    // Comparison verdicts. These deliberately sit outside the palette:
    // the swap delta is the point of the mod and needs to stand out, not blend in.
    public static final int BETTER = 0x7BD88F;
    public static final int WORSE = 0xE8788A;
    public static final int NEUTRAL = 0x9A9A9A;
}