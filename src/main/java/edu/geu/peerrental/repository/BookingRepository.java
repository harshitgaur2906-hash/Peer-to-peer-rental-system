package edu.geu.peerrental.repository;

import edu.geu.peerrental.model.Booking;
import edu.geu.peerrental.model.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            select b from Booking b join fetch b.item i join fetch i.owner join fetch b.renter
            where b.renter.id = :userId order by b.createdAt desc
            """)
    List<Booking> findSent(@Param("userId") Long userId);

    @Query("""
            select b from Booking b join fetch b.item i join fetch i.owner join fetch b.renter
            where i.owner.id = :userId order by b.createdAt desc
            """)
    List<Booking> findReceived(@Param("userId") Long userId);

    @Query("""
            select b from Booking b join fetch b.item i join fetch i.owner join fetch b.renter
            where b.id = :id
            """)
    Optional<Booking> findWithDetails(@Param("id") Long id);

    /** Locks the booking row so two people can't change the same request at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    /** Date ranges overlap when start <= otherEnd AND end >= otherStart (inclusive days). */
    @Query("""
            select count(b) from Booking b
            where b.item.id = :itemId and b.status in :statuses
              and b.startDate <= :end and b.endDate >= :start
              and b.id <> :excludeId
            """)
    long countOverlapping(@Param("itemId") Long itemId,
                          @Param("statuses") Collection<BookingStatus> statuses,
                          @Param("start") LocalDate start,
                          @Param("end") LocalDate end,
                          @Param("excludeId") Long excludeId);

    @Query("""
            select count(b) from Booking b
            where b.item.id = :itemId and b.renter.id = :renterId and b.status in :statuses
              and b.startDate <= :end and b.endDate >= :start
            """)
    long countRenterOverlapping(@Param("itemId") Long itemId,
                                @Param("renterId") Long renterId,
                                @Param("statuses") Collection<BookingStatus> statuses,
                                @Param("start") LocalDate start,
                                @Param("end") LocalDate end);

    @Query("""
            select count(b) from Booking b
            where b.item.id = :itemId and b.status in :statuses and b.endDate >= :today
            """)
    long countActiveForItem(@Param("itemId") Long itemId,
                            @Param("statuses") Collection<BookingStatus> statuses,
                            @Param("today") LocalDate today);

    @Modifying
    @Query("delete from Booking b where b.item.id = :itemId")
    void deleteByItemId(@Param("itemId") Long itemId);
}
