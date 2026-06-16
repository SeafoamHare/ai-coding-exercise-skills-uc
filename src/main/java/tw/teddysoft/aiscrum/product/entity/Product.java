package tw.teddysoft.aiscrum.product.entity;

import tw.teddysoft.aiscrum.common.entity.DateProvider;
import tw.teddysoft.ezddd.entity.EsAggregateRoot;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static tw.teddysoft.ucontract.Contract.*;

public class Product extends EsAggregateRoot<ProductId, ProductEvents> {

    public final static String CATEGORY = "Product";

    private ProductId id;
    private ProductName name;
    private ProductGoal goal;
    private String note;
    private String extension;
    private ProductLifecycleState state;

    // Event replay constructor — required by EsRepository to rebuild from the event store.
    public Product(List<ProductEvents> domainEvents) {
        super(domainEvents);
    }

    // Business constructor — used by CreateProductService to create a new product.
    public Product(ProductId productId, ProductName name) {
        super();
        requireNotNull("Product id", productId);
        requireNotNull("Product name", name);

        apply(new ProductEvents.ProductCreated(
                productId,
                name,
                null,
                null,
                ProductLifecycleState.DRAFT.name(),
                new HashMap<>(),
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure("Product id is set correctly", () -> _idMatches(productId));
        ensure("Product name is set correctly", () -> _nameMatches(name));
        ensure("Product state is DRAFT initially", () -> this.state == ProductLifecycleState.DRAFT);
        ensure("A ProductCreated event is generated correctly", () ->
                _productCreatedEventGenerated(productId, name));
    }

    public ProductName getName() {
        return name;
    }

    /**
     * Returns the product goal as a read-only view (Read-only Entities pattern; aggregate.md Rule 13).
     * ProductGoal is a child entity crossing the aggregate boundary, so the live entity must never
     * be handed out — clients receive a {@link ReadOnlyProductGoal} that rejects mutation.
     */
    public ProductGoal getGoal() {
        return goal == null ? null : new ReadOnlyProductGoal(goal);
    }

    public String getNote() {
        return note;
    }

    public String getExtension() {
        return extension;
    }

    public ProductLifecycleState getState() {
        return state;
    }

    @Override
    public ProductId getId() {
        return id;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public void ensureInvariant() {
        invariant("Category is '" + CATEGORY + "'", this::_categoryMatches);
        invariantNotNull("Product Id", id);
        if (!isDeleted) {
            invariantNotNull("Product name", name);
            invariantNotNull("Product state", state);
        }
    }

    @Override
    protected void when(ProductEvents event) {
        switch (event) {
            case ProductEvents.ProductCreated e -> {
                this.id = e.productId();
                this.name = e.name();
                this.goal = null;
                this.note = e.note();
                this.extension = e.extension();
                this.state = ProductLifecycleState.valueOf(e.state());
            }
        }
    }

    private boolean _idMatches(ProductId expected) {
        return Objects.equals(this.id, expected);
    }

    private boolean _nameMatches(ProductName expected) {
        return Objects.equals(this.name, expected);
    }

    private boolean _categoryMatches() {
        return getCategory().equals(CATEGORY);
    }

    private boolean _productCreatedEventGenerated(ProductId productId, ProductName name) {
        return getLastDomainEvent()
                .filter(event -> event instanceof ProductEvents.ProductCreated)
                .map(ProductEvents.ProductCreated.class::cast)
                .map(created -> created.productId().equals(productId)
                        && created.name().equals(name)
                        && created.state().equals(ProductLifecycleState.DRAFT.name()))
                .orElse(false);
    }
}
