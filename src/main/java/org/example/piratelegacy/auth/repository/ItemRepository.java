package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByItemKey(String itemKey);
}