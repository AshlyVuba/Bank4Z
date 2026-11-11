package com.bank4z.backend.fraudservice;

import com.bank4z.backend.fraudservice.dto.FraudFlagResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Lives under /api/admin, so SecurityConfig's URL rule already requires
 * ROLE_ADMIN; @PreAuthorize is the second, independent layer.
 */
@RestController
@RequestMapping("/api/admin/fraud-flags")
@PreAuthorize("hasRole('ADMIN')")
public class FraudAdminController {

    private static final int MAX_PAGE_SIZE = 50;

    private final FraudService fraudService;

    public FraudAdminController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    /** ?resolved=false shows only what still needs review; omit it to see everything. */
    @GetMapping
    public Page<FraudFlagResponse> list(
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return fraudService.listFlags(resolved, pageable);
    }

    @PostMapping("/{id}/resolve")
    public FraudFlagResponse resolve(@PathVariable UUID id) {
        return fraudService.resolveFlag(id);
    }
}