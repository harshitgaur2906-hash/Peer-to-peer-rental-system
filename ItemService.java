package edu.geu.peerrental.service;

import edu.geu.peerrental.dto.Dtos.*;
import edu.geu.peerrental.model.BookingStatus;
import edu.geu.peerrental.model.Item;
import edu.geu.peerrental.repository.BookingRepository;
import edu.geu.peerrental.repository.ItemRepository;
import edu.geu.peerrental.repository.UserRepository;
import edu.geu.peerrental.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ItemService {
    private static final BigDecimal NO_LIMIT = new BigDecimal("99999999");

    private final ItemRepository items;
    private final UserRepository users;
    private final BookingRepository bookings;

    public ItemService(ItemRepository items, UserRepository users, BookingRepository bookings) {
        this.items = items;
        this.users = users;
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> search(String q, String category, BigDecimal maxPrice) {
        String query = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (query.length() > 100) query = query.substring(0, 100);
        String cat = (category == null) ? "" : category.trim();
        BigDecimal max = (maxPrice == null) ? NO_LIMIT : maxPrice;
        return items.search(query, cat, max).stream().map(ItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> mine(Long userId) {
        return items.findByOwnerIdOrderByCreatedAtDesc(userId).stream().map(ItemResponse::from).toList();
    }

    @Transactional
    public ItemResponse create(Long userId, ItemRequest r) {
        Item item = new Item();
        item.setOwner(users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Please log in first")));
        apply(item, r);
        item.setAvailable(r.available() == null || r.available());
        return ItemResponse.from(items.save(item));
    }

    @Transactional
    public ItemResponse update(Long userId, Long id, ItemRequest r) {
        Item item = owned(userId, id);
        apply(item, r);
        if (r.available() != null) item.setAvailable(r.available());
        return ItemResponse.from(item);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Item item = owned(userId, id);
        long active = bookings.countActiveForItem(id, Set.of(BookingStatus.PENDING, BookingStatus.APPROVED), LocalDate.now());
        if (active > 0) {
            throw ApiException.conflict("This item has pending or approved bookings. Decline them first, "
                    + "or mark the item as unavailable instead.");
        }
        bookings.deleteByItemId(id);
        items.delete(item);
    }

    private Item owned(Long userId, Long id) {
        Item item = items.findWithOwner(id).orElseThrow(() -> ApiException.notFound("Item not found"));
        if (!item.getOwner().getId().equals(userId)) {
            throw ApiException.forbidden("You can only change your own listings");
        }
        return item;
    }

    private static void apply(Item item, ItemRequest r) {
        item.setTitle(r.title().trim());
        item.setDescription(blankToNull(r.description()));
        item.setCategory(r.category().trim());
        item.setPricePerDay(r.pricePerDay());
        item.setLocation(blankToNull(r.location()));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
