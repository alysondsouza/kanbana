package com.kanbana.infrastructure.persistence;

import com.kanbana.domain.model.Card;
import com.kanbana.domain.repository.CardRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class CardRepositoryAdapter implements CardRepository {

    private final CardJpaRepository jpa;

    public CardRepositoryAdapter(CardJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Card save(Card card) {
        CardEntity entity = CardMapper.toEntity(card);
        CardEntity saved = jpa.save(entity);
        return CardMapper.toDomain(saved);
    }

    @Override
    public Optional<Card> findById(UUID id) {
        return jpa.findById(id).map(CardMapper::toDomain);
    }

    @Override
    public List<Card> findByColumnId(UUID columnId) {
        return jpa.findByColumnId(columnId)
                  .stream()
                  .map(CardMapper::toDomain)
                  .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }
}
