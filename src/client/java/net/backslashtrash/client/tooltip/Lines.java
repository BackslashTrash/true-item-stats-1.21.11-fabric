package net.backslashtrash.client.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

/**
 * Builders for formatted tooltip lines. Formatting lives here so the stat
 * builders stay concerned with numbers.
 */
public final class Lines {

    private Lines() {
    }

    /** A "Label: value" line with independently coloured halves. */
    public static Component stat(String label, String value, int labelRgb, int valueRgb) {
        return Component.literal(label + ": ")
                .withStyle(s -> s.withColor(TextColor.fromRgb(labelRgb)))
                .append(Component.literal(value)
                        .withStyle(s -> s.withColor(TextColor.fromRgb(valueRgb))));
    }

    /** An indented sub-line, for situational or per-profile detail. */
    public static Component sub(String label, String value, int labelRgb, int valueRgb) {
        return stat("  " + label, value, labelRgb, valueRgb);
    }

    public static Component plain(String text, int rgb) {
        return Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(rgb)));
    }

    public static Component hint(String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC);
    }

    /** Signed number, coloured green for better and red for worse. */
    public static Component delta(String label, double value, String unit) {
        int colour = value > 0.01 ? Palette.BETTER
                : value < -0.01 ? Palette.WORSE
                : Palette.NEUTRAL;
        String sign = value > 0.01 ? "+" : "";
        return stat(label, String.format("%s%.1f%s", sign, value, unit),
                Palette.ARMOUR_LABEL, colour);
    }

    public static String percent(double value) {
        return String.format("%.1f%%", value);
    }

    public static String multiplier(double value) {
        return Double.isInfinite(value) ? "immune" : String.format("x%.1f", value);
    }
}