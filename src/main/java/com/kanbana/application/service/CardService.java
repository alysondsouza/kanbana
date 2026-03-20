package com.kanbana.application.service;

import com.kanbana.application.dto.CardResponseDTO;
import com.kanbana.application.dto.CreateCardRequestDTO;
import com.kanbana.application.dto.MoveCardRequestDTO;
import com.kanbana.application.dto.UpdateCardRequestDTO;
import com.kanbana.domain.model.Card;
import com.kanbana.domain.repository.CardRepository;
import com.kanbana.domain.repository.ColumnRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final ColumnRepository columnRepository;  // needed to validate column exists

    public CardService(CardRepository cardRepository, ColumnRepository columnRepository) {
        this.cardRepository = cardRepository;
        this.columnRepository = columnRepository;
    }

    public CardResponseDTO createCard(UUID columnId, CreateCardRequestDTO request) {
        // Business rule: cannot add a card to a non-existent column
        if (!columnRepository.existsById(columnId)) {
            throw new EntityNotFoundException("Column not found: " + columnId);
        }

        int position = cardRepository.findByColumnId(columnId).size();

        Card card = new Card(
            UUID.randomUUID(),
            request.getTitle(),
            request.getDescription(),
            position,
            columnId
        );
        Card saved = cardRepository.save(card);
        return CardResponseDTO.from(saved);
    }

    public List<CardResponseDTO> getCardsByColumn(UUID columnId) {
        if (!columnRepository.existsById(columnId)) {
            throw new EntityNotFoundException("Column not found: " + columnId);
        }
        return cardRepository.findByColumnId(columnId)
            .stream()
            .map(CardResponseDTO::from)
            .collect(Collectors.toList());
    }

    public CardResponseDTO updateCard(UUID cardId, UpdateCardRequestDTO request) {
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));

        // Only update fields that were actually provided (non-null)
        if (request.getTitle() != null)       card.setTitle(request.getTitle());
        if (request.getDescription() != null) card.setDescription(request.getDescription());

        Card saved = cardRepository.save(card);
        return CardResponseDTO.from(saved);
    }

    public CardResponseDTO moveCard(UUID cardId, MoveCardRequestDTO request) {
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));

        UUID targetColumnId = request.getTargetColumnId();

        // Business rule: target column must exist
        if (!columnRepository.existsById(targetColumnId)) {
            throw new EntityNotFoundException("Target column not found: " + targetColumnId);
        }

        // Move card — position appends to end of target column
        int newPosition = cardRepository.findByColumnId(targetColumnId).size();
        card.setColumnId(targetColumnId);
        card.setPosition(newPosition);

        Card saved = cardRepository.save(card);
        return CardResponseDTO.from(saved);
    }

    public void deleteCard(UUID cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new EntityNotFoundException("Card not found: " + cardId);
        }
        cardRepository.deleteById(cardId);
    }
}
