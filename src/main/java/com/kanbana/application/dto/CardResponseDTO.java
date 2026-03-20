package com.kanbana.application.dto;

import com.kanbana.domain.model.Card;
import java.util.UUID;

public class CardResponseDTO {

    private UUID id;
    private String title;
    private String description;
    private int position;
    private UUID columnId;

    public CardResponseDTO() {}

    public static CardResponseDTO from(Card card) {
        CardResponseDTO response = new CardResponseDTO();
        response.id = card.getId();
        response.title = card.getTitle();
        response.description = card.getDescription();
        response.position = card.getPosition();
        response.columnId = card.getColumnId();
        return response;
    }

    public UUID getId()            { return id; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }
    public int getPosition()       { return position; }
    public UUID getColumnId()      { return columnId; }
}
