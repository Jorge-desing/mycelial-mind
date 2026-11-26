package com.alex.mycelialmind.util;

import java.util.EnumSet;

/**
 * Pure-Java helper for compact integer bitmask encoding and decoding of enum sets.
 */
public final class EnumBitmask {
    private EnumBitmask() {
    }

    public static <E extends Enum<E>> int encode(Iterable<E> values) {
        if (values == null) {
            return 0;
        }
        int mask = 0;
        for (E value : values) {
            if (value != null) {
                mask |= (1 << value.ordinal());
            }
        }
        return mask;
    }

    public static <E extends Enum<E>> EnumSet<E> decode(Class<E> enumClass, int mask) {
        EnumSet<E> result = EnumSet.noneOf(enumClass);
        for (E value : enumClass.getEnumConstants()) {
            if ((mask & (1 << value.ordinal())) != 0) {
                result.add(value);
            }
        }
        return result;
    }

    public static <E extends Enum<E>> boolean has(int mask, E value) {
        return value != null && (mask & (1 << value.ordinal())) != 0;
    }

    public static <E extends Enum<E>> int with(int mask, E value) {
        return value == null ? mask : (mask | (1 << value.ordinal()));
    }
}
