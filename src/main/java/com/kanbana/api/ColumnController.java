package com.kanbana.api;

import com.kanbana.application.dto.ColumnResponseDTO;
import com.kanbana.application.dto.CreateColumnRequestDTO;
import com.kanbana.application.service.ColumnService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ColumnController {

    private final ColumnService columnService;

    public ColumnController(ColumnService columnService) {
        this.columnService = columnService;
    }

    // POST /api/v1/boards/{boardId}/columns → 201 Created
    @PostMapping("/api/v1/boards/{boardId}/columns")
    public ResponseEntity<ColumnResponseDTO> addColumn(
            @PathVariable UUID boardId,
            @Valid @RequestBody CreateColumnRequestDTO request) {
        ColumnResponseDTO response = columnService.addColumn(boardId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/boards/{boardId}/columns → 200 OK
    @GetMapping("/api/v1/boards/{boardId}/columns")
    public ResponseEntity<List<ColumnResponseDTO>> getColumns(@PathVariable UUID boardId) {
        return ResponseEntity.ok(columnService.getColumnsByBoard(boardId));
    }

    // DELETE /api/v1/columns/{id} → 204 No Content
    @DeleteMapping("/api/v1/columns/{id}")
    public ResponseEntity<Void> deleteColumn(@PathVariable UUID id) {
        columnService.deleteColumn(id);
        return ResponseEntity.noContent().build();
    }
}
