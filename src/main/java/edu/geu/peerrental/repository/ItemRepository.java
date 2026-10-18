package edu.geu.peerrental.repository;

import edu.geu.peerrental.model.Item;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    /** Empty string for q / category means "no filter" (avoids NULL-parameter typing problems). */
    @Query("""
            select i from Item i join fetch i.owner
            where i.available = true
              and (:category = '' or i.category = :category)
              and i.pricePerDay <= :maxPrice
              and (:q = '' or lower(i.title) like concat('%', :q, '%')
                           or lower(coalesce(i.description, '')) like concat('%', :q, '%'))
            order by i.createdAt desc
            """)
    List<Item> search(@Param("q") String q,
                      @Param("category") String category,
                      @Param("maxPrice") BigDecimal maxPrice);

    @EntityGraph(attributePaths = "owner")
    List<Item> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("select i from Item i join fetch i.owner where i.id = :id")
    Optional<Item> findWithOwner(@Param("id") Long id);

    /** Row lock (SELECT ... FOR UPDATE) — serialises approvals for the same item. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);
}
