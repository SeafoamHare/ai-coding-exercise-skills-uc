package tw.teddysoft.aiscrum.product.entity;

import tw.teddysoft.ezddd.entity.ValueObject;

public enum ProductGoalState implements ValueObject {
    PLANNED,
    ACTIVE,
    ACHIEVED,
    SUPERSEDED,
    CANCELLED
}
