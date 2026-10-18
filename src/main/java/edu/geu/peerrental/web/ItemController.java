package edu.geu.peerrental.web;

import edu.geu.peerrental.dto.Dtos.*;
import edu.geu.peerrental.service.ItemService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemService items;
    private final SessionAuth auth;

    public ItemController(ItemService items, SessionAuth auth) {
        this.items = items;
        this.auth = auth;
    }

    @GetMapping
    public List<ItemResponse> browse(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) BigDecimal maxPrice,
                                     HttpServletRequest req) {
        auth.requireUserId(req);
        return items.search(q, category, maxPrice);
    }

    @GetMapping("/mine")
    public List<ItemResponse> mine(HttpServletRequest req) {
        return items.mine(auth.requireUserId(req));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@Valid @RequestBody ItemRequest r, HttpServletRequest req) {
        return items.create(auth.requireUserId(req), r);
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id, @Valid @RequestBody ItemRequest r, HttpServletRequest req) {
        return items.update(auth.requireUserId(req), id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest req) {
        items.delete(auth.requireUserId(req), id);
    }
}
