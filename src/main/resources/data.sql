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

INSERT INTO customer (email, password, name, registration_date, birth_date, phone_number, address) VALUES
('john.doe@example.com', 'hashed_password123', 'John Doe', '2023-01-01', '1990-05-15', '123-456-7890', '123 Main St'),
('jane.doe@example.com', 'hashed_password456', 'Jane Doe', '2023-02-01', '2000-08-20', '987-654-3210', '456 Elm St'),
('alice.smith@example.com', 'hashed_password789', 'Alice Smith', '2023-03-01', '2010-12-10', '555-123-4567', '789 Oak St');
