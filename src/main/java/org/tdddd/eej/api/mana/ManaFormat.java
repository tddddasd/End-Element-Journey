package org.tdddd.eej.api.mana;

import java.util.Locale;


/**
 * The mod's canonical mana number format.
 *
 * <p>Values of 1000 mana and above are abbreviated with the uppercase suffix {@code K} / {@code M} /
 * {@code G} (thousand / million / billion) and two decimals ({@code 2147483647} -> {@code 2.15G mana}).
 * Below 1000 the number stays a plain integer. {@link #exact(int)} is the unabbreviated form and is
 * what JEI uses, so a recipe's cost is always shown as a real number.</p>
 */
public final class ManaFormat {

    
    public static final String UNIT = "mana";

    private static final String[] SUFFIXES = {"", "K", "M", "G"};

    private ManaFormat() {
    }

    
    public static String number(int value) {
        long remaining = value;
        String sign = "";
        if (remaining < 0) {
            sign = "-";
            remaining = -remaining;
        }
        int tier = 0;
        long divisor = 1L;
        while (tier < SUFFIXES.length - 1 && remaining >= divisor * 1000L) {
            divisor *= 1000L;
            tier++;
        }
        if (tier == 0) {
            return sign + remaining;
        }
        return sign + String.format(Locale.ROOT, "%.2f%s", (double) remaining / (double) divisor, SUFFIXES[tier]);
    }

    
    public static String abbreviated(int value) {
        return number(value) + " " + UNIT;
    }

    
    public static String exact(int value) {
        return Integer.toString(value);
    }

    
    public static String bar(int current, int max) {
        return number(current) + "/" + number(max) + " " + UNIT;
    }
}
