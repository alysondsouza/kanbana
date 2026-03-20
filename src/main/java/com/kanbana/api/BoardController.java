package com.kanbana.api;

import com.kanbana.application.dto.BoardResponseDTO;
import com.kanbana.application.dto.CreateBoardRequestDTO;
import com.kanbana.application.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    // POST /api/v1/boards → 201 Created
    @PostMapping
    public ResponseEntity<BoardResponseDTO> createBoard(
            @Valid @RequestBody CreateBoardRequestDTO request) {
        BoardResponseDTO response = boardService.createBoard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/boards → 200 OK
    @GetMapping
    public ResponseEntity<List<BoardResponseDTO>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    // GET /api/v1/boards/{id} → 200 OK
    @GetMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> getBoardById(@PathVariable UUID id) {
        return ResponseEntity.ok(boardService.getBoardById(id));
    }

    // DELETE /api/v1/boards/{id} → 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable UUID id) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }
}
