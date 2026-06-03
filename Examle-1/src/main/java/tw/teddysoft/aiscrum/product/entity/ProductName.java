package tw.teddysoft.aiscrum.product.entity;

import tw.teddysoft.ezddd.entity.ValueObject;

import java.util.Objects;
import java.util.UUID;

public record ProductName(String value) implements ValueObject {
    public ProductName {
        Objects.requireNonNull(value, "ProductName cannot be null");
    }

    public static ProductName valueOf(String value) {
        return new ProductName(value);
    }

    public static ProductName valueOf(UUID value) {
        return new ProductName(value.toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
