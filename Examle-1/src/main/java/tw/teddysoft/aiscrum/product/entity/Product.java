package tw.teddysoft.aiscrum.product.entity;

import tw.teddysoft.ezddd.entity.EsAggregateRoot;
import tw.teddysoft.aiscrum.common.entity.DateProvider;

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

    public Product(List<ProductEvents> domainEvents) {
        super(domainEvents);
    }

    public Product(ProductId productId, ProductName name) {
        super();
        requireNotNull("productId", productId);
        requireNotNull("name", name);

        apply(new ProductEvents.ProductCreated(
                productId,
                name,
                null,
                null,
                null,
                ProductLifecycleState.DRAFT.name(),
                new HashMap<>(),
                UUID.randomUUID(),
                DateProvider.now()
        ));

        ensure("ID matches", () -> this.id.equals(productId));
        ensure("Name matches", () -> _nameMatches(name));
        ensure("State is DRAFT", () -> this.state == ProductLifecycleState.DRAFT);
    }

    @Override
    protected void when(ProductEvents event) {
        switch (event) {
            case ProductEvents.ProductCreated e -> when(e);
        }
    }

    private void when(ProductEvents.ProductCreated event) {
        this.id = event.productId();
        this.name = event.name();
        this.goal = event.goal();
        this.note = event.note();
        this.extension = event.extension();
        this.state = ProductLifecycleState.valueOf(event.state());
    }

    @Override
    public ProductId getId() {
        return id;
    }

    public ProductName getName() {
        return name;
    }

    public ProductGoal getGoal() {
        return goal;
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
    public String getCategory() {
        return CATEGORY;
    }

    private boolean _nameMatches(ProductName expected) {
        return Objects.equals(this.name, expected);
    }
}
