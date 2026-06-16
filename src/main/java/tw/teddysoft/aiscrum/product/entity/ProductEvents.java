package tw.teddysoft.aiscrum.product.entity;

import tw.teddysoft.ezddd.entity.DomainEventTypeMapper;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public sealed interface ProductEvents extends InternalDomainEvent {

    ProductId productId();

    @Override
    default String source() {
        return productId().value();
    }

    String MAPPING_TYPE_PREFIX = "ProductEvents$";

    record ProductCreated(
            ProductId productId,
            ProductName name,
            String note,
            String extension,
            String state,
            Map<String, String> metadata,
            UUID id,
            Instant occurredOn
    ) implements ProductEvents, InternalDomainEvent.ConstructionEvent {

        public ProductCreated {
            Objects.requireNonNull(productId);
            Objects.requireNonNull(name);
            // note and extension are nullable
            Objects.requireNonNull(state);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }

        @Override
        public Map<String, String> metadata() {
            return metadata;
        }
    }

    static DomainEventTypeMapper mapper() {
        DomainEventTypeMapper mapper = DomainEventTypeMapper.create();
        mapper.put(MAPPING_TYPE_PREFIX + "ProductCreated", ProductCreated.class);
        return mapper;
    }
}
