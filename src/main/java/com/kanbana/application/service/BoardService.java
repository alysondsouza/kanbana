package com.kanbana.application.service;

import com.kanbana.application.dto.BoardResponseDTO;
import com.kanbana.application.dto.CreateBoardRequestDTO;
import com.kanbana.domain.model.Board;
import com.kanbana.domain.repository.BoardRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BoardService {

    private final BoardRepository boardRepository;

    // Constructor injection — preferred over @Autowired on field
    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    public BoardResponseDTO createBoard(CreateBoardRequestDTO request) {
        Board board = new Board(
            UUID.randomUUID(),      // id generated here in the service
            request.getTitle(),
            UUID.randomUUID(),      // placeholder ownerId until auth is implemented
            Instant.now()           // createdAt generated here
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
        return boardRepository.findAll()
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
}
