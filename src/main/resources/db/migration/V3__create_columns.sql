CREATE TABLE kanbana.board_columns (
    id       UUID         PRIMARY KEY,
    title    VARCHAR(255) NOT NULL,
    position INT          NOT NULL DEFAULT 0,
    board_id UUID         NOT NULL REFERENCES boards(id) ON DELETE CASCADE
);
