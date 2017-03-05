DROP ALL OBJECTS;

CREATE TABLE Client (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    first_name VARCHAR(512) NOT NULL,
    last_name VARCHAR(512) NOT NULL
);

CREATE TABLE ClientBalance(
    client_id BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount DECIMAL(20, 2) NOT NULL
);

ALTER TABLE ClientBalance ADD PRIMARY KEY(client_id, currency, amount);

CREATE TABLE Transaction (
    id UUID NOT NULL,
    client_id BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount DECIMAL(20, 2) NOT NULL,
    peer BIGINT NOT NULL,
    date_time DATETIME NOT NULL
);

ALTER TABLE Transaction ADD PRIMARY KEY(client_id, id);

CREATE INDEX Transaction_idx ON Transaction(client_id, currency);

--todo INDEXES