package com.kanbana.application.service;

import com.kanbana.application.dto.BoardResponseDTO;
import com.kanbana.application.dto.CreateBoardRequestDTO;
import com.kanbana.domain.model.Board;
import com.kanbana.domain.repository.BoardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// @ExtendWith wires Mockito into JUnit 5 — no Spring context loaded, no DB, very fast
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    // @Mock creates a fake BoardRepository — calls return null/empty by default
    @Mock
    private BoardRepository boardRepository;

    // @InjectMocks creates a real BoardService and injects the mock above into it
    @InjectMocks
    private BoardService boardService;

    // ── createBoard ──────────────────────────────────────────────────────────

    @Test
    void createBoard_happyPath_returnsDTOWithCorrectTitle() {
        // ARRANGE — build the DTO the controller would pass in
        CreateBoardRequestDTO request = new CreateBoardRequestDTO();
        request.setTitle("Sprint 1");

        // Stub: when save() is called with any Board, return that same Board back
        // (mirrors what the real repository does after persisting)
        when(boardRepository.save(any(Board.class)))
            .thenAnswer(inv -> inv.getArgument(0)); // return the exact object passed in

        // ACT
        BoardResponseDTO result = boardService.createBoard(request);

        // ASSERT
        assertThat(result.getTitle()).isEqualTo("Sprint 1");
        assertThat(result.getId()).isNotNull();        // service generated a UUID
        assertThat(result.getOwnerId()).isNotNull();   // placeholder UUID also generated
        verify(boardRepository).save(any(Board.class)); // save was called exactly once
    }

    // ── getBoardById ─────────────────────────────────────────────────────────

    @Test
    void getBoardById_found_returnsDTO() {
        // ARRANGE
        UUID id = UUID.randomUUID();
        Board board = new Board(id, "My Board", UUID.randomUUID(), Instant.now());

        when(boardRepository.findById(id)).thenReturn(Optional.of(board));

        // ACT
        BoardResponseDTO result = boardService.getBoardById(id);

        // ASSERT
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getTitle()).isEqualTo("My Board");
    }

    @Test
    void getBoardById_notFound_throwsEntityNotFoundException() {
        // ARRANGE — stub returns empty: board does not exist
        UUID id = UUID.randomUUID();
        when(boardRepository.findById(id)).thenReturn(Optional.empty());

        // ASSERT — assertThatThrownBy catches the exception and lets us inspect it
        assertThatThrownBy(() -> boardService.getBoardById(id))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(id.toString());
    }

    // ── deleteBoard ──────────────────────────────────────────────────────────

    @Test
    void deleteBoard_happyPath_callsDeleteById() {
        // ARRANGE — board exists
        UUID id = UUID.randomUUID();
        when(boardRepository.existsById(id)).thenReturn(true);

        // ACT
        boardService.deleteBoard(id);

        // ASSERT — deleteById was called with the correct id
        verify(boardRepository).deleteById(id);
    }

    @Test
    void deleteBoard_notFound_throwsEntityNotFoundException() {
        // ARRANGE — board does not exist
        UUID id = UUID.randomUUID();
        when(boardRepository.existsById(id)).thenReturn(false);

        // ASSERT
        assertThatThrownBy(() -> boardService.deleteBoard(id))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(id.toString());

        // deleteById must NEVER be called when the board doesn't exist
        verify(boardRepository, never()).deleteById(any());
    }
}
