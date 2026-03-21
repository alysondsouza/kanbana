package com.kanbana.application.service;

import com.kanbana.application.dto.BoardResponseDTO;
import com.kanbana.application.dto.CreateBoardRequestDTO;
import com.kanbana.domain.model.Board;
import com.kanbana.domain.repository.BoardRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BoardService {

    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    public BoardResponseDTO createBoard(CreateBoardRequestDTO request) {
        Board board = new Board(
            UUID.randomUUID(),
            request.getTitle(),
            getCurrentUserId(),     // real authenticated user ID — replaces UUID.randomUUID()
            Instant.now()
        );
        Board saved = boardRepository.save(board);
        return BoardResponseDTO.from(saved);
    }

    public BoardResponseDTO getBoardById(UUID id) {
        Board board = boardRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Board not found: " + id));
        return BoardResponseDTO.from(board);
    }

    public List<BoardResponseDTO> getAllBoards() {
        // Only return boards owned by the authenticated user
        return boardRepository.findByOwnerId(getCurrentUserId())
            .stream()
            .map(BoardResponseDTO::from)
            .collect(Collectors.toList());
    }

    public void deleteBoard(UUID id) {
        if (!boardRepository.existsById(id)) {
            throw new EntityNotFoundException("Board not found: " + id);
        }
        boardRepository.deleteById(id);
    }

    // Reads the authenticated user's UUID from SecurityContext.
    // JwtAuthFilter sets this as the principal on every valid request.
    private UUID getCurrentUserId() {
        return (UUID) SecurityContextHolder.getContext()
                                           .getAuthentication()
                                           .getPrincipal();
    }
}
