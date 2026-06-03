package tw.teddysoft.aiscrum.product.entity;

import tw.teddysoft.ezddd.entity.ValueObject;

public enum ProductLifecycleState implements ValueObject {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    DEPRECATED,
    EOL,
    ARCHIVED
}
