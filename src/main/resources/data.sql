DROP TABLE IF EXISTS login_attempt;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS customer;

CREATE TABLE customer (
    email VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255),
    name VARCHAR(255),
    registration_date DATE,
    birth_date DATE,
    phone_number VARCHAR(20),
    address VARCHAR(255)
);

CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    reset_token VARCHAR(255) NOT NULL,
    token_expiry BIGINT NOT NULL,
    FOREIGN KEY (email) REFERENCES customer(email) ON DELETE CASCADE
);

CREATE TABLE login_attempt (
    email VARCHAR(255) PRIMARY KEY,
    attempt_count INT NOT NULL DEFAULT 0,
    locked_until BIGINT,
    last_attempt_time BIGINT NOT NULL
);

INSERT INTO customer (email, password, name, registration_date, birth_date, phone_number, address) VALUES
('john.doe@example.com', 'hashed_password123', 'John Doe', '2023-01-01', '1990-05-15', '123-456-7890', '123 Main St'),
('jane.doe@example.com', 'hashed_password456', 'Jane Doe', '2023-02-01', '2000-08-20', '987-654-3210', '456 Elm St'),
('alice.smith@example.com', 'hashed_password789', 'Alice Smith', '2023-03-01', '2010-12-10', '555-123-4567', '789 Oak St'),
('bob.johnson@example.com', 'hashed_password101', 'Bob Johnson', '2023-04-01', '1985-03-25', '111-222-3333', '321 Pine St'),
('carol.white@example.com', 'hashed_password202', 'Carol White', '2023-05-01', '1995-07-30', '222-333-4444', '654 Maple St'),
('david.brown@example.com', 'hashed_password303', 'David Brown', '2023-06-01', '1988-11-12', '333-444-5555', '987 Cedar St'),
('emma.davis@example.com', 'hashed_password404', 'Emma Davis', '2023-07-01', '1992-02-18', '444-555-6666', '147 Birch St'),
('frank.miller@example.com', 'hashed_password505', 'Frank Miller', '2023-08-01', '1998-09-05', '555-666-7777', '258 Spruce St'),
('grace.wilson@example.com', 'hashed_password606', 'Grace Wilson', '2023-09-01', '2005-04-22', '666-777-8888', '369 Ash St'),
('henry.moore@example.com', 'hashed_password707', 'Henry Moore', '2023-10-01', '1987-06-14', '777-888-9999', '741 Willow St'),
('iris.taylor@example.com', 'hashed_password808', 'Iris Taylor', '2023-11-01', '1993-10-28', '888-999-0000', '852 Poplar St'),
('jack.anderson@example.com', 'hashed_password909', 'Jack Anderson', '2023-12-01', '1991-01-09', '999-000-1111', '963 Hickory St'),
('karen.thomas@example.com', 'hashed_password010', 'Karen Thomas', '2024-01-01', '1996-08-17', '000-111-2222', '159 Chestnut St'),
('leo.jackson@example.com', 'hashed_password111', 'Leo Jackson', '2024-02-01', '2002-03-31', '111-222-3333', '357 Walnut St'),
('maria.harris@example.com', 'hashed_password212', 'Maria Harris', '2024-03-01', '1989-12-24', '222-333-4444', '468 Sycamore St');

