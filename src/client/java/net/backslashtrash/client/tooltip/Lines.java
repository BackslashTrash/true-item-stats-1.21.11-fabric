package net.backslashtrash.client.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

/**
 * Builders for formatted tooltip lines. Formatting lives here so the stat
 * builders stay concerned with numbers.
 */
public final class Lines {

    /**
     * Smallest change worth calling a change.
     *
     * <p>Tied to the display format below: it sits just under the smallest value
     * two decimals can show, so nothing ever renders as a non-zero number coloured
     * as if it were neutral. Change the format and change this with it.
     */
    public static final double EPSILON = 0.005;

    private Lines() {
    }

    /** A "Label: value" line with independently coloured halves. */
    public static Component stat(String label, String value, int labelRgb, int valueRgb) {
        return Component.literal(label + ": ")
                .withStyle(s -> s.withColor(TextColor.fromRgb(labelRgb)))
                .append(Component.literal(value)
                        .withStyle(s -> s.withColor(TextColor.fromRgb(valueRgb))));
    }

    /** An indented sub-line, for per-damage-type detail. */
    public static Component sub(String label, String value, int labelRgb, int valueRgb) {
        return stat("  " + label, value, labelRgb, valueRgb);
    }

    public static Component plain(String text, int rgb) {
        return Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(rgb)));
    }

    public static Component hint(String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC);
    }

    /**
     * A relative change, coloured green for better and red for worse.
     *
     * <p>Relative rather than absolute on purpose: effective health gains are
     * convex, so a first piece of armour adds far less raw effective health than a
     * fourth one does. As a ratio it reads the same whether you are naked or in
     * full netherite.
     */
    public static Component deltaPercent(String label, double percent) {
        int colour = percent > EPSILON ? Palette.BETTER
                : percent < -EPSILON ? Palette.WORSE
                : Palette.NEUTRAL;
        return stat(label, signedPercent(percent), Palette.ARMOUR_LABEL, colour);
    }

    /** Signed percentage at two decimals, e.g. "+1.63% health". */
    public static String signedPercent(double percent) {
        String sign = percent > EPSILON ? "+" : "";
        return String.format("%s%.2f%% health", sign, percent);
    }

    /** Colour for a change of this size and direction. */
    public static int colourFor(double change) {
        return change > EPSILON ? Palette.BETTER
                : change < -EPSILON ? Palette.WORSE
                : Palette.ARMOUR_VALUE;
    }

    public static String percent(double value) {
        return String.format("%.2f%%", value);
    }

    public static String multiplier(double value) {
        return Double.isInfinite(value) ? "immune" : String.format("x%.2f", value);
    }
}