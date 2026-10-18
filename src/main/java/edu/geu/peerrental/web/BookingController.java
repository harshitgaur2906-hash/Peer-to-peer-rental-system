package edu.geu.peerrental.web;

import edu.geu.peerrental.dto.Dtos.*;
import edu.geu.peerrental.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookings;
    private final SessionAuth auth;

    public BookingController(BookingService bookings, SessionAuth auth) {
        this.bookings = bookings;
        this.auth = auth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest r, HttpServletRequest req) {
        return bookings.create(auth.requireUserId(req), r);
    }

    /** Requests I sent as a renter. */
    @GetMapping("/mine")
    public List<BookingResponse> mine(HttpServletRequest req) {
        return bookings.sent(auth.requireUserId(req));
    }

    /** Requests other students sent for MY items. */
    @GetMapping("/received")
    public List<BookingResponse> received(HttpServletRequest req) {
        return bookings.received(auth.requireUserId(req));
    }

    @PatchMapping("/{id}/status")
    public BookingResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest r, HttpServletRequest req) {
        return bookings.changeStatus(auth.requireUserId(req), id, r.status());
    }
}
