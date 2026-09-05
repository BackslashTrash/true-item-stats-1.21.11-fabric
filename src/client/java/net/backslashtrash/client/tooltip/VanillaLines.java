package net.backslashtrash.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;

/**
 * Finds and removes vanilla's attribute modifier block from a tooltip.
 *
 * <p>Lines are identified by translation key, never by rendered text, so this
 * keeps working in every language.
 */
public final class VanillaLines {

    private static final String HEADER_KEY = "item.modifiers.";
    private static final String MODIFIER_KEY = "attribute.modifier.";

    private VanillaLines() {
    }

    /**
     * Whether this component, or any component nested inside it, uses a
     * translation key with the given prefix.
     *
     * <p>The recursion is essential: vanilla builds modifier lines as a leading
     * space with the translatable appended as a sibling, so the root component's
     * contents are plain text and a top-level check misses them entirely.
     */
    private static boolean hasKeyPrefix(Component component, String prefix) {
        if (component.getContents() instanceof TranslatableContents t
                && t.getKey().startsWith(prefix)) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (hasKeyPrefix(sibling, prefix)) return true;
        }
        return false;
    }

    /** Index of the first "When in Main Hand" style header, or -1. */
    public static int findHeader(List<Component> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (hasKeyPrefix(lines.get(i), HEADER_KEY)) return i;
        }
        return -1;
    }

    /**
     * Deletes the header and its modifier lines, plus the blank line above it.
     *
     * @return the index where replacement content should be inserted
     */
    public static int removeBlock(List<Component> lines, int headerIndex) {
        int end = headerIndex + 1;
        while (end < lines.size() && hasKeyPrefix(lines.get(end), MODIFIER_KEY)) {
            end++;
        }
        lines.subList(headerIndex, end).clear();

        if (headerIndex > 0 && lines.get(headerIndex - 1).getString().isEmpty()) {
            lines.remove(headerIndex - 1);
            return headerIndex - 1;
        }
        return headerIndex;
    }
}