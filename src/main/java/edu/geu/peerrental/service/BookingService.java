package edu.geu.peerrental.service;

import edu.geu.peerrental.dto.Dtos.*;
import edu.geu.peerrental.model.Booking;
import edu.geu.peerrental.model.BookingStatus;
import edu.geu.peerrental.model.Item;
import edu.geu.peerrental.model.User;
import edu.geu.peerrental.repository.BookingRepository;
import edu.geu.peerrental.repository.ItemRepository;
import edu.geu.peerrental.repository.UserRepository;
import edu.geu.peerrental.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {
    private static final int MAX_DAYS = 90;
    private static final Set<BookingStatus> APPROVED_ONLY = Set.of(BookingStatus.APPROVED);
    private static final Set<BookingStatus> OPEN = Set.of(BookingStatus.PENDING, BookingStatus.APPROVED);

    private final BookingRepository bookings;
    private final ItemRepository items;
    private final UserRepository users;

    public BookingService(BookingRepository bookings, ItemRepository items, UserRepository users) {
        this.bookings = bookings;
        this.items = items;
        this.users = users;
    }

    @Transactional
    public BookingResponse create(Long renterId, BookingRequest r) {
        LocalDate start = r.startDate();
        LocalDate end = r.endDate();
        if (start.isBefore(LocalDate.now())) throw ApiException.badRequest("Start date cannot be in the past");
        if (end.isBefore(start)) throw ApiException.badRequest("End date must be on or after the start date");
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_DAYS) throw ApiException.badRequest("Maximum rental period is " + MAX_DAYS + " days");

        Item item = items.findWithOwner(r.itemId()).orElseThrow(() -> ApiException.notFound("Item not found"));
        if (!item.isAvailable()) throw ApiException.conflict("This item is not available right now");
        if (item.getOwner().getId().equals(renterId)) throw ApiException.badRequest("You cannot rent your own item");
        if (bookings.countOverlapping(item.getId(), APPROVED_ONLY, start, end, -1L) > 0) {
            throw ApiException.conflict("This item is already booked for those dates");
        }
        if (bookings.countRenterOverlapping(item.getId(), renterId, OPEN, start, end) > 0) {
            throw ApiException.conflict("You already have a request for this item on overlapping dates");
        }

        User renter = users.findById(renterId).orElseThrow(() -> ApiException.unauthorized("Please log in first"));
        Booking b = new Booking();
        b.setItem(item);
        b.setRenter(renter);
        b.setStartDate(start);
        b.setEndDate(end);
        b.setTotalPrice(item.getPricePerDay().multiply(BigDecimal.valueOf(days)));
        b.setStatus(BookingStatus.PENDING);
        String msg = r.message();
        b.setMessage(msg == null || msg.isBlank() ? null : msg.trim());
        return BookingResponse.from(bookings.save(b));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> sent(Long userId) {
        return bookings.findSent(userId).stream().map(BookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> received(Long userId) {
        return bookings.findReceived(userId).stream().map(BookingResponse::from).toList();
    }

    /**
     * Critical section: lock the booking row, then its item row. Two owners/threads can't approve
     * overlapping bookings at the same time, because the second one waits for the lock and then
     * sees the first approval in the overlap check.
     */
    @Transactional
    public BookingResponse changeStatus(Long userId, Long bookingId, BookingStatus target) {
        Booking b = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        Item item = items.findByIdForUpdate(b.getItem().getId())
                .orElseThrow(() -> ApiException.notFound("Item not found"));

        boolean isOwner = item.getOwner().getId().equals(userId);
        boolean isRenter = b.getRenter().getId().equals(userId);

        switch (target) {
            case APPROVED, DECLINED -> {
                if (!isOwner) throw ApiException.forbidden("Only the item owner can approve or decline");
                if (b.getStatus() != BookingStatus.PENDING) {
                    throw ApiException.conflict("Only pending requests can be approved or declined");
                }
                if (target == BookingStatus.APPROVED) {
                    if (b.getEndDate().isBefore(LocalDate.now())) {
                        throw ApiException.conflict("These dates are already in the past");
                    }
                    if (!item.isAvailable()) throw ApiException.conflict("This item is marked unavailable");
                    if (bookings.countOverlapping(item.getId(), APPROVED_ONLY,
                            b.getStartDate(), b.getEndDate(), b.getId()) > 0) {
                        throw ApiException.conflict("Another approved booking already overlaps these dates");
                    }
                }
            }
            case CANCELLED -> {
                if (!isRenter) throw ApiException.forbidden("Only the renter can cancel a request");
                if (!OPEN.contains(b.getStatus())) {
                    throw ApiException.conflict("This request can no longer be cancelled");
                }
            }
            default -> throw ApiException.badRequest("Status must be APPROVED, DECLINED or CANCELLED");
        }

        b.setStatus(target);
        return BookingResponse.from(b);
    }
}
