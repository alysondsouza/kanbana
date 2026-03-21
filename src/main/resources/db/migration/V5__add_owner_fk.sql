ALTER TABLE kanbana.boards
    ADD CONSTRAINT boards_owner_id_fkey
    FOREIGN KEY (owner_id) REFERENCES kanbana.users(id);
