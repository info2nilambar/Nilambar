ALTER TABLE products
    ADD COLUMN unit_cost DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN reorder_level INT NOT NULL DEFAULT 10,
    ADD COLUMN last_restocked_at DATETIME(6) NULL;

UPDATE products SET unit_cost = ROUND(price * 0.65, 2), reorder_level = 15;

CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    team VARCHAR(60) NOT NULL,
    designation VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    hired_on DATE NOT NULL,
    exited_on DATE NULL,
    weekly_capacity_hours INT NOT NULL DEFAULT 40,
    performance_score DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employees_code (code),
    KEY idx_employees_status (status),
    KEY idx_employees_team (team)
) ENGINE=InnoDB;

CREATE TABLE attendance_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    hours_worked DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    overtime_hours DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_employee_date (employee_id, work_date),
    KEY idx_attendance_date (work_date),
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;

CREATE TABLE projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_on DATE NOT NULL,
    planned_end_on DATE NOT NULL,
    actual_end_on DATE NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_projects_code (code)
) ENGINE=InnoDB;

CREATE TABLE employee_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    title VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL,
    due_on DATE NOT NULL,
    completed_on DATE NULL,
    estimated_hours DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    KEY idx_tasks_employee (employee_id),
    KEY idx_tasks_status (status),
    CONSTRAINT fk_tasks_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB;

CREATE TABLE training_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    course_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    completed_on DATE NULL,
    PRIMARY KEY (id),
    KEY idx_training_employee (employee_id),
    CONSTRAINT fk_training_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;

CREATE TABLE vendors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    contact_email VARCHAR(160) NOT NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    avg_response_minutes INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vendors_name (name)
) ENGINE=InnoDB;

CREATE TABLE purchase_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    po_number VARCHAR(32) NOT NULL,
    vendor_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_cost DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ordered_at DATETIME(6) NOT NULL,
    expected_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchase_orders_number (po_number),
    KEY idx_purchase_orders_vendor (vendor_id),
    CONSTRAINT fk_purchase_orders_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT fk_purchase_orders_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;

CREATE TABLE stock_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    unit_cost DECIMAL(12, 2) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    reference VARCHAR(64),
    PRIMARY KEY (id),
    KEY idx_stock_movements_product (product_id, occurred_at),
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;
