package tw.teddysoft.aiscrum.product.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.description = description;
        this.metrics = new ArrayList<>();
        this.definedAt = Objects.requireNonNull(definedAt, "DefinedAt cannot be null");
        this.revisedAt = null;
        this.state = ProductGoalState.PLANNED;
    }

    public ProductGoalId getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<GoalMetric> getMetrics() { return metrics; }
    public Instant getDefinedAt() { return definedAt; }
    public Instant getRevisedAt() { return revisedAt; }
    public ProductGoalState getState() { return state; }

    void revise(String title, String description, Instant revisedAt) {
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.description = description;
        this.revisedAt = revisedAt;
    }

    void addMetric(GoalMetric metric) {
        this.metrics.add(Objects.requireNonNull(metric, "GoalMetric cannot be null"));
    }

    void changeState(ProductGoalState newState) {
        this.state = Objects.requireNonNull(newState, "State cannot be null");
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
