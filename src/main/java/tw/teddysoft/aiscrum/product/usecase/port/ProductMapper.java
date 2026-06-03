package tw.teddysoft.aiscrum.product.usecase.port;

import tw.teddysoft.aiscrum.common.entity.DateProvider;
import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.aiscrum.product.entity.ProductName;
import tw.teddysoft.aiscrum.product.usecase.port.out.ProductData;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxMapper;

import java.util.Objects;
import java.util.stream.Collectors;

public class ProductMapper implements OutboxMapper<Product, ProductData> {

    public static ProductMapper newMapper() {
        return new ProductMapper();
    }

    @Override
    public Product toDomain(ProductData data) {
        Objects.requireNonNull(data, "ProductData cannot be null");

        Product product = new Product(
                ProductId.valueOf(data.getProductId()),
                ProductName.valueOf(data.getName())
        );

        product.setVersion(data.getVersion());
        product.clearDomainEvents();
        return product;
    }

    @Override
    public ProductData toData(Product product) {
        Objects.requireNonNull(product, "Product cannot be null");

        ProductData data = new ProductData(product.getVersion());
        data.setId(product.getId().toString());
        data.setName(product.getName().toString());
        data.setNote(product.getNote());
        data.setExtension(product.getExtension());
        data.setState(product.getState().name());
        data.setDeleted(product.isDeleted());
        data.setCreatedAt(extractCreatedAt(product));
        data.setLastUpdated(extractLastUpdated(product));
        data.setStreamName(product.getStreamName());
        data.setDomainEventDatas(
                product.getDomainEvents().stream()
                        .map(DomainEventMapper::toData)
                        .collect(Collectors.toList())
        );

        return data;
    }

    private static java.time.Instant extractCreatedAt(Product product) {
        if (!product.getDomainEvents().isEmpty()) {
            return product.getDomainEvents().get(0).occurredOn();
        }
        return DateProvider.now();
    }

    private static java.time.Instant extractLastUpdated(Product product) {
        if (!product.getDomainEvents().isEmpty()) {
            return product.getDomainEvents().get(product.getDomainEvents().size() - 1).occurredOn();
        }
        return DateProvider.now();
    }
}
