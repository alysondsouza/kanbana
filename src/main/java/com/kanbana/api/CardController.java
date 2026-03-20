package com.kanbana.api;

import com.kanbana.application.dto.CardResponseDTO;
import com.kanbana.application.dto.CreateCardRequestDTO;
import com.kanbana.application.dto.MoveCardRequestDTO;
import com.kanbana.application.dto.UpdateCardRequestDTO;
import com.kanbana.application.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    // POST /api/v1/columns/{columnId}/cards → 201 Created
    @PostMapping("/api/v1/columns/{columnId}/cards")
    public ResponseEntity<CardResponseDTO> createCard(
            @PathVariable UUID columnId,
            @Valid @RequestBody CreateCardRequestDTO request) {
        CardResponseDTO response = cardService.createCard(columnId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/columns/{columnId}/cards → 200 OK
    @GetMapping("/api/v1/columns/{columnId}/cards")
    public ResponseEntity<List<CardResponseDTO>> getCards(@PathVariable UUID columnId) {
        return ResponseEntity.ok(cardService.getCardsByColumn(columnId));
    }

    // PATCH /api/v1/cards/{id} → 200 OK
    @PatchMapping("/api/v1/cards/{id}")
    public ResponseEntity<CardResponseDTO> updateCard(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCardRequestDTO request) {
        return ResponseEntity.ok(cardService.updateCard(id, request));
    }

    // PATCH /api/v1/cards/{id}/move → 200 OK
    @PatchMapping("/api/v1/cards/{id}/move")
    public ResponseEntity<CardResponseDTO> moveCard(
            @PathVariable UUID id,
            @Valid @RequestBody MoveCardRequestDTO request) {
        return ResponseEntity.ok(cardService.moveCard(id, request));
    }

    // DELETE /api/v1/cards/{id} → 204 No Content
    @DeleteMapping("/api/v1/cards/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
