CREATE TABLE customer (
    email VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255),
    name VARCHAR(255),
    registration_date DATE
);

INSERT INTO customer (email, password, name, registration_date) VALUES
('john.doe@example.com', 'password123', 'John Doe', '2023-01-01'),
('jane.doe@example.com', 'password456', 'Jane Doe', '2023-02-01'),
('alice.smith@example.com', 'password789', 'Alice Smith', '2023-03-01');
