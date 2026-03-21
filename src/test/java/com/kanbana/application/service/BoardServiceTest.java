package com.kanbana.application.service;

import com.kanbana.application.dto.BoardResponseDTO;
import com.kanbana.application.dto.CreateBoardRequestDTO;
import com.kanbana.domain.model.Board;
import com.kanbana.domain.repository.BoardRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock private BoardRepository boardRepository;
    @InjectMocks private BoardService boardService;

    private final UUID ownerId = UUID.randomUUID();

    // Set a fake authenticated user in SecurityContext before each test
    @BeforeEach
    void setUpSecurityContext() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(ownerId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // Clear SecurityContext after each test — avoid leaking state between tests
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── createBoard ──────────────────────────────────────────────────────────

    @Test
    void createBoard_happyPath_returnsDTOWithCorrectTitle() {
        CreateBoardRequestDTO request = new CreateBoardRequestDTO();
        request.setTitle("Sprint 1");

        when(boardRepository.save(any(Board.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        BoardResponseDTO result = boardService.createBoard(request);

        assertThat(result.getTitle()).isEqualTo("Sprint 1");
        assertThat(result.getId()).isNotNull();
        assertThat(result.getOwnerId()).isEqualTo(ownerId); // real owner, not random
        verify(boardRepository).save(any(Board.class));
    }

    // ── getBoardById ─────────────────────────────────────────────────────────

    @Test
    void getBoardById_found_returnsDTO() {
        UUID id = UUID.randomUUID();
        Board board = new Board(id, "My Board", ownerId, Instant.now());

        when(boardRepository.findById(id)).thenReturn(Optional.of(board));

        BoardResponseDTO result = boardService.getBoardById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getTitle()).isEqualTo("My Board");
    }

    @Test
    void getBoardById_notFound_throwsEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(boardRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getBoardById(id))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(id.toString());
    }

    // ── deleteBoard ──────────────────────────────────────────────────────────

    @Test
    void deleteBoard_happyPath_callsDeleteById() {
        UUID id = UUID.randomUUID();
        when(boardRepository.existsById(id)).thenReturn(true);

        boardService.deleteBoard(id);

        verify(boardRepository).deleteById(id);
    }

    @Test
    void deleteBoard_notFound_throwsEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(boardRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> boardService.deleteBoard(id))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(id.toString());

        verify(boardRepository, never()).deleteById(any());
    }
}
