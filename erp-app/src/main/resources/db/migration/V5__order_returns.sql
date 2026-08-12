CREATE TABLE order_returns (
    id BIGINT NOT NULL AUTO_INCREMENT,
    return_number VARCHAR(32) NOT NULL,
    order_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(30) NOT NULL,
    comment VARCHAR(1000),
    refund_amount DECIMAL(12, 2) NOT NULL,
    refund_reference VARCHAR(64),
    resolution_note VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_returns_number (return_number),
    KEY idx_order_returns_order (order_id),
    CONSTRAINT fk_order_returns_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE TABLE return_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    return_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    refund_amount DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_return_items_return (return_id),
    CONSTRAINT fk_return_items_return FOREIGN KEY (return_id) REFERENCES order_returns (id),
    CONSTRAINT fk_return_items_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id)
) ENGINE=InnoDB;
