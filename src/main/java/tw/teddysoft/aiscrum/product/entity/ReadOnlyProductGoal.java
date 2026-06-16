package tw.teddysoft.aiscrum.product.entity;

import java.time.Instant;

/**
 * Read-only view of {@link ProductGoal} exposed across the Product aggregate boundary
 * (Read-only Entities pattern, Special Case — JISE 42(4), 2026 §2.4.1).
 *
 * Command methods throw {@link UnsupportedOperationException} — this is a deliberate LSP
 * trade-off (expected behavior, NOT a contract violation). Query methods returning
 * immutable values (and the unmodifiable metrics collection) are inherited unchanged.
 */
public final class ReadOnlyProductGoal extends ProductGoal {

    private static final String MSG =
            "ProductGoal is read-only when exposed outside the Product aggregate; "
                    + "modifications must go through the Product aggregate root.";

    public ReadOnlyProductGoal(ProductGoal source) {
        super(source);
    }

    @Override
    void revise(String title, String description, Instant revisedAt) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    void addMetric(GoalMetric metric) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    void changeState(ProductGoalState state) {
        throw new UnsupportedOperationException(MSG);
    }
}
