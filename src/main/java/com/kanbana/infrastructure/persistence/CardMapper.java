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
	    null,   // null → INSERT; Spring Data generates the UUID
            card.getTitle(),
            card.getDescription(),
            card.getPosition(),
            card.getColumnId()
        );
    }
}
