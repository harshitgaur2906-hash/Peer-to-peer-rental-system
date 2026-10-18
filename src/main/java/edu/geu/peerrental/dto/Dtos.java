package edu.geu.peerrental.dto;

import edu.geu.peerrental.model.Booking;
import edu.geu.peerrental.model.BookingStatus;
import edu.geu.peerrental.model.Item;
import edu.geu.peerrental.model.User;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Dtos {
    private Dtos() {}

    // ---------- requests ----------
    public record RegisterRequest(
            @NotBlank(message = "Name is required") @Size(max = 100) String name,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 150) String email,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {}

    public record ItemRequest(
            @NotBlank(message = "Title is required") @Size(max = 150) String title,
            @Size(max = 2000) String description,
            @NotBlank(message = "Category is required") @Size(max = 50) String category,
            @NotNull(message = "Price per day is required")
            @DecimalMin(value = "0.01", message = "Price must be greater than 0")
            @Digits(integer = 8, fraction = 2, message = "Price format is invalid") BigDecimal pricePerDay,
            @Size(max = 150) String location,
            Boolean available) {}

    public record BookingRequest(
            @NotNull(message = "Item is required") Long itemId,
            @NotNull(message = "Start date is required") LocalDate startDate,
            @NotNull(message = "End date is required") LocalDate endDate,
            @Size(max = 500) String message) {}

    public record StatusRequest(@NotNull(message = "Status is required") BookingStatus status) {}

    // ---------- responses ----------
    public record UserResponse(Long id, String name, String email) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail());
        }
    }

    public record ItemResponse(Long id, String title, String description, String category,
                               BigDecimal pricePerDay, String location, boolean available,
                               Long ownerId, String ownerName, LocalDateTime createdAt) {
        public static ItemResponse from(Item i) {
            return new ItemResponse(i.getId(), i.getTitle(), i.getDescription(), i.getCategory(),
                    i.getPricePerDay(), i.getLocation(), i.isAvailable(),
                    i.getOwner().getId(), i.getOwner().getName(), i.getCreatedAt());
        }
    }

    public record BookingResponse(Long id, Long itemId, String itemTitle,
                                  Long ownerId, String ownerName,
                                  Long renterId, String renterName,
                                  LocalDate startDate, LocalDate endDate,
                                  BigDecimal totalPrice, BookingStatus status,
                                  String message, LocalDateTime createdAt) {
        public static BookingResponse from(Booking b) {
            Item i = b.getItem();
            return new BookingResponse(b.getId(), i.getId(), i.getTitle(),
                    i.getOwner().getId(), i.getOwner().getName(),
                    b.getRenter().getId(), b.getRenter().getName(),
                    b.getStartDate(), b.getEndDate(), b.getTotalPrice(), b.getStatus(),
                    b.getMessage(), b.getCreatedAt());
        }
    }
}
