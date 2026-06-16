package tw.teddysoft.aiscrum.product.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Product goal — a mutable child entity within the Product aggregate.
 *
 * Non-final with package-private mutators so {@link ReadOnlyProductGoal} can override them
 * (Read-only Entities pattern; entity.md Rule 11). The collection getter returns an
 * unmodifiable view to avoid leaking the live internal list across the aggregate boundary.
 */
public class ProductGoal {
    private final ProductGoalId id;
    private String title;
    private String description;
    private final List<GoalMetric> metrics;
    private final Instant definedAt;
    private Instant revisedAt;
    private ProductGoalState state;

    public ProductGoal(ProductGoalId id, String title, String description, Instant definedAt) {
        this.id = Objects.requireNonNull(id, "ProductGoalId cannot be null");
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.definedAt = Objects.requireNonNull(definedAt, "definedAt cannot be null");
        this.metrics = new ArrayList<>();
        this.revisedAt = definedAt;
        this.state = ProductGoalState.PLANNED;
    }

    // Copy constructor for the Read-only Special Case (entity.md Rule 11).
    // Directly copies fields (does NOT call mutators — subclass overrides them to throw).
    protected ProductGoal(ProductGoal source) {
        Objects.requireNonNull(source, "source ProductGoal cannot be null");
        this.id = source.id;
        this.title = source.title;
        this.description = source.description;
        this.metrics = new ArrayList<>(source.metrics);
        this.definedAt = source.definedAt;
        this.revisedAt = source.revisedAt;
        this.state = source.state;
    }

    public ProductGoalId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<GoalMetric> getMetrics() {
        return Collections.unmodifiableList(metrics);
    }

    public Instant getDefinedAt() {
        return definedAt;
    }

    public Instant getRevisedAt() {
        return revisedAt;
    }

    public ProductGoalState getState() {
        return state;
    }

    void revise(String title, String description, Instant revisedAt) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.revisedAt = Objects.requireNonNull(revisedAt, "revisedAt cannot be null");
    }

    void addMetric(GoalMetric metric) {
        this.metrics.add(Objects.requireNonNull(metric, "metric cannot be null"));
    }

    void changeState(ProductGoalState state) {
        this.state = Objects.requireNonNull(state, "state cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductGoal that = (ProductGoal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
