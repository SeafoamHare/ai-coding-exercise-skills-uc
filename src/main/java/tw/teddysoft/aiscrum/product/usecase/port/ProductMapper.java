package tw.teddysoft.aiscrum.product.usecase.port;

import tw.teddysoft.aiscrum.common.entity.DateProvider;
import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.aiscrum.product.entity.ProductName;
import tw.teddysoft.aiscrum.product.usecase.port.out.ProductData;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxMapper;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProductMapper {

    private static final OutboxMapper<Product, ProductData> mapper = new Mapper();

    public static OutboxMapper<Product, ProductData> newMapper() {
        return mapper;
    }

    public static ProductData toData(Product product) {
        Objects.requireNonNull(product, "Product cannot be null");

        ProductData data = new ProductData(product.getVersion());
        data.setId(product.getId().toString());
        data.setProductName(product.getName().toString());
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

    public static Product toDomain(ProductData data) {
        Objects.requireNonNull(data, "ProductData cannot be null");

        // ① Basic fields — Business Constructor
        Product product = new Product(
                ProductId.valueOf(data.getId()),
                ProductName.valueOf(data.getProductName())
        );

        // ② Version + clearDomainEvents() — always the last two lines
        product.setVersion(data.getVersion());
        product.clearDomainEvents();
        return product;
    }

    private static Instant extractCreatedAt(Product product) {
        if (!product.getDomainEvents().isEmpty()) {
            return product.getDomainEvents().get(0).occurredOn();
        }
        return DateProvider.now();
    }

    private static Instant extractLastUpdated(Product product) {
        if (!product.getDomainEvents().isEmpty()) {
            return product.getDomainEvents()
                    .get(product.getDomainEvents().size() - 1).occurredOn();
        }
        return DateProvider.now();
    }

    static class Mapper implements OutboxMapper<Product, ProductData> {
        @Override
        public Product toDomain(ProductData data) {
            return ProductMapper.toDomain(data);
        }

        @Override
        public ProductData toData(Product aggregateRoot) {
            return ProductMapper.toData(aggregateRoot);
        }
    }
}
