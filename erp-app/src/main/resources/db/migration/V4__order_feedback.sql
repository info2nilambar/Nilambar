CREATE TABLE order_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_feedback_order (order_id),
    CONSTRAINT fk_order_feedback_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;
