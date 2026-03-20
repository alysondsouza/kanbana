package com.kanbana.application.service;

import com.kanbana.application.dto.ColumnResponseDTO;
import com.kanbana.application.dto.CreateColumnRequestDTO;
import com.kanbana.domain.model.BoardColumn;
import com.kanbana.domain.repository.BoardRepository;
import com.kanbana.domain.repository.ColumnRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ColumnService {

    private final ColumnRepository columnRepository;
    private final BoardRepository boardRepository;  // needed to validate board exists

    public ColumnService(ColumnRepository columnRepository, BoardRepository boardRepository) {
        this.columnRepository = columnRepository;
        this.boardRepository = boardRepository;
    }

    public ColumnResponseDTO addColumn(UUID boardId, CreateColumnRequestDTO request) {
        // Business rule: cannot add a column to a non-existent board
        if (!boardRepository.existsById(boardId)) {
            throw new EntityNotFoundException("Board not found: " + boardId);
        }

        // Position = current column count (appends to end)
        int position = columnRepository.findByBoardId(boardId).size();

        BoardColumn column = new BoardColumn(
            UUID.randomUUID(),
            request.getTitle(),
            position,
            boardId
        );
        BoardColumn saved = columnRepository.save(column);
        return ColumnResponseDTO.from(saved);
    }

    public List<ColumnResponseDTO> getColumnsByBoard(UUID boardId) {
        if (!boardRepository.existsById(boardId)) {
            throw new EntityNotFoundException("Board not found: " + boardId);
        }
        return columnRepository.findByBoardId(boardId)
            .stream()
            .map(ColumnResponseDTO::from)
            .collect(Collectors.toList());
    }

    public void deleteColumn(UUID columnId) {
        if (!columnRepository.existsById(columnId)) {
            throw new EntityNotFoundException("Column not found: " + columnId);
        }
        columnRepository.deleteById(columnId);
    }
}
