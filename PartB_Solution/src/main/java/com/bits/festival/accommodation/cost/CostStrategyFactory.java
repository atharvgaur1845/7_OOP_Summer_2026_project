package com.bits.festival.accommodation.cost;

/**
 * Factory (GoF) for {@link CostStrategy} instances. Centralising construction lets the rest
 * of the engine ask for a strategy <em>by name</em> (e.g. from a CLI flag or GUI dropdown)
 * without depending on concrete classes, and makes it trivial to register experimental cost
 * models later.
 */
public final class CostStrategyFactory {

    /** Available cost models. */
    public enum Type {
        DEFAULT
    }

    private CostStrategyFactory() {
    }

    public static CostStrategy create(Type type) {
        switch (type) {
            case DEFAULT:
                return new DefaultCostStrategy();
            default:
                throw new IllegalArgumentException("Unknown cost strategy: " + type);
        }
    }

    /** Lenient name-based lookup (falls back to DEFAULT for unknown / null names). */
    public static CostStrategy create(String name) {
        if (name == null || name.isBlank()) {
            return create(Type.DEFAULT);
        }
        try {
            return create(Type.valueOf(name.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return create(Type.DEFAULT);
        }
    }

    public static CostStrategy createDefault() {
        return create(Type.DEFAULT);
    }
}
