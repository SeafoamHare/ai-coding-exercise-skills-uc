package tw.teddysoft.aiscrum.product.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tw.teddysoft.ucontract.PreconditionViolationException;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contract tests for the Product aggregate (Design by Contract).
 * Pure JUnit 5 — no Spring required.
 */
@DisplayName("Product Aggregate Contracts")
public class ProductContractTest {

    // ========== Helper Methods ==========

    private Product createProduct() {
        return new Product(ProductId.create(), ProductName.valueOf("Test Product"));
    }

    private ProductGoal goalWithMetric() {
        ProductGoal goal = new ProductGoal(
                ProductGoalId.create(), "Increase NPS", "Improve customer satisfaction", Instant.now());
        goal.addMetric(new GoalMetric("NPS", "score", new BigDecimal("80"), new BigDecimal("50"), true));
        return goal;
    }

    // ========== Constructor Null Preconditions ==========

    @Nested
    @DisplayName("Constructor Null Preconditions")
    class ConstructorNullPreconditions {

        @Test
        @DisplayName("constructor rejects null product id")
        void constructor_rejects_null_productId() {
            assertThatThrownBy(() -> new Product(null, ProductName.valueOf("Test Product")))
                    .isInstanceOf(PreconditionViolationException.class)
                    .hasMessageContaining("Product id");
        }

        @Test
        @DisplayName("constructor rejects null product name")
        void constructor_rejects_null_name() {
            assertThatThrownBy(() -> new Product(ProductId.create(), null))
                    .isInstanceOf(PreconditionViolationException.class)
                    .hasMessageContaining("Product name");
        }
    }

    // ========== Constructor Postconditions ==========

    @Nested
    @DisplayName("Constructor Postconditions")
    class ConstructorPostconditions {

        @Test
        @DisplayName("constructor ensures id is set correctly")
        void constructor_ensures_id_is_set() {
            ProductId id = ProductId.create();

            Product product = new Product(id, ProductName.valueOf("Test Product"));

            assertThat(product.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("constructor ensures name is set correctly")
        void constructor_ensures_name_is_set() {
            ProductName name = ProductName.valueOf("Test Product");

            Product product = new Product(ProductId.create(), name);

            assertThat(product.getName()).isEqualTo(name);
        }

        @Test
        @DisplayName("constructor ensures state is DRAFT initially")
        void constructor_ensures_state_is_DRAFT() {
            Product product = createProduct();

            assertThat(product.getState()).isEqualTo(ProductLifecycleState.DRAFT);
        }

        @Test
        @DisplayName("constructor emits ProductCreated event")
        void constructor_emits_ProductCreated_event() {
            Product product = createProduct();

            assertThat(product.getDomainEvents())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(ProductEvents.ProductCreated.class);
        }

        @Test
        @DisplayName("ProductCreated event contains correct id, name and state")
        void ProductCreated_event_contains_correct_data() {
            ProductId id = ProductId.create();
            ProductName name = ProductName.valueOf("Test Product");

            Product product = new Product(id, name);

            ProductEvents.ProductCreated event = (ProductEvents.ProductCreated)
                    product.getDomainEvents().get(0);
            assertThat(event.productId()).isEqualTo(id);
            assertThat(event.name()).isEqualTo(name);
            assertThat(event.state()).isEqualTo(ProductLifecycleState.DRAFT.name());
        }
    }

    // ========== getGoal() Read-only Exposure ==========

    @Nested
    @DisplayName("getGoal() returns a read-only view")
    class GetGoalReadOnly {

        @Test
        @DisplayName("getGoal() returns null when no goal is set")
        void getGoal_returns_null_when_absent() {
            Product product = createProduct();

            assertThat(product.getGoal()).isNull();
        }
    }

    // ========== ReadOnlyProductGoal — Read-only Entities pattern ==========

    @Nested
    @DisplayName("ReadOnlyProductGoal — Read-only Entities pattern")
    class ReadOnlyExposure {

        @Test
        @DisplayName("type is preserved (still a ProductGoal)")
        void type_preserved() {
            assertThat(new ReadOnlyProductGoal(goalWithMetric()))
                    .isInstanceOf(ProductGoal.class);
        }

        @Test
        @DisplayName("query methods return the same values as the source")
        void queries_inherited() {
            ProductGoal source = goalWithMetric();
            ReadOnlyProductGoal readOnly = new ReadOnlyProductGoal(source);

            assertThat(readOnly.getTitle()).isEqualTo(source.getTitle());
            assertThat(readOnly.getState()).isEqualTo(source.getState());
            assertThat(readOnly.getMetrics()).hasSize(1);
        }

        @Test
        @DisplayName("command methods throw UnsupportedOperationException (fail-fast)")
        void commands_forbidden() {
            ReadOnlyProductGoal readOnly = new ReadOnlyProductGoal(goalWithMetric());

            assertThatThrownBy(() -> readOnly.revise("x", "y", Instant.now()))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> readOnly.addMetric(
                    new GoalMetric("CSAT", "percent", new BigDecimal("90"), new BigDecimal("70"), false)))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> readOnly.changeState(ProductGoalState.ACTIVE))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("returned collection is unmodifiable (no live-reference leak)")
        void collection_unmodifiable() {
            ReadOnlyProductGoal readOnly = new ReadOnlyProductGoal(goalWithMetric());

            assertThatThrownBy(() -> readOnly.getMetrics().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
