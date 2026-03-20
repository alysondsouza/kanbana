CREATE TABLE kanbana.boards (
    id         UUID         PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    owner_id   UUID         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
