package tw.teddysoft.aiscrum.product.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tw.teddysoft.ucontract.PreconditionViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Product Aggregate Contracts")
public class ProductContractTest {

    @Nested
    @DisplayName("Constructor Contracts")
    class ConstructorContracts {

        @Test
        @DisplayName("constructor rejects null productId")
        void constructor_rejects_null_productId() {
            assertThatThrownBy(() -> new Product(null, ProductName.valueOf("Test")))
                    .isInstanceOf(PreconditionViolationException.class)
                    .hasMessageContaining("productId");
        }

        @Test
        @DisplayName("constructor rejects null name")
        void constructor_rejects_null_name() {
            assertThatThrownBy(() -> new Product(ProductId.create(), null))
                    .isInstanceOf(PreconditionViolationException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("constructor ensures ID is set correctly")
        void constructor_ensures_id_is_set() {
            ProductId productId = ProductId.create();
            Product product = new Product(productId, ProductName.valueOf("Test"));

            assertThat(product.getId()).isEqualTo(productId);
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
            Product product = new Product(ProductId.create(), ProductName.valueOf("Test"));

            assertThat(product.getState()).isEqualTo(ProductLifecycleState.DRAFT);
        }

        @Test
        @DisplayName("constructor emits ProductCreated event")
        void constructor_emits_ProductCreated_event() {
            ProductId productId = ProductId.create();
            ProductName name = ProductName.valueOf("Test");

            Product product = new Product(productId, name);

            assertThat(product.getDomainEvents())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(ProductEvents.ProductCreated.class);

            ProductEvents.ProductCreated event = (ProductEvents.ProductCreated) product.getDomainEvents().get(0);
            assertThat(event.productId()).isEqualTo(productId);
            assertThat(event.name()).isEqualTo(name);
            assertThat(event.state()).isEqualTo(ProductLifecycleState.DRAFT.name());
        }
    }
}
