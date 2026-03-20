package com.kanbana.domain.repository;

import com.kanbana.domain.model.Card;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository {
    Card save(Card card);
    Optional<Card> findById(UUID id);
    List<Card> findByColumnId(UUID columnId);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
