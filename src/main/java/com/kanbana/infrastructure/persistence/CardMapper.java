package com.kanbana.infrastructure.persistence;

import com.kanbana.domain.model.Card;

public class CardMapper {

    private CardMapper() {}

    public static Card toDomain(CardEntity entity) {
        return new Card(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getPosition(),
            entity.getColumnId()
        );
    }

    public static CardEntity toEntity(Card card) {
        return new CardEntity(
            card.getId(),       // null on new cards (triggers INSERT), real ID on updates (triggers UPDATE)
            card.getTitle(),
            card.getDescription(),
            card.getPosition(),
            card.getColumnId()
        );
    }
}
