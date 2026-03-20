CREATE TABLE kanbana.cards (
    id          UUID         PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    position    INT          NOT NULL DEFAULT 0,
    column_id   UUID         NOT NULL REFERENCES board_columns(id) ON DELETE CASCADE
);
