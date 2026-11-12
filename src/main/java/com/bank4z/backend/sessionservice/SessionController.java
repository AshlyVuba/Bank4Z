package com.bank4z.backend.sessionservice;

import com.bank4z.backend.authservice.UserPrincipal;
import com.bank4z.backend.sessionservice.dto.SessionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    public record RevokedCountResponse(int count) {}

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** The logged-in user's active devices. "current" marks the one making this request. */
    @GetMapping
    public List<SessionResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return sessionService.activeSessions(principal.getId()).stream()
                .map(session -> SessionResponse.from(session, principal.getSessionId()))
                .toList();
    }

    /** Log out THIS device (the app calls this when you tap log out). */
    @DeleteMapping("/current")
    public ResponseEntity<Void> logoutCurrent(@AuthenticationPrincipal UserPrincipal principal) {
        sessionService.revoke(principal.getId(), principal.getSessionId());
        return ResponseEntity.noContent().build();
    }

    /** Log out one specific device. A session that isn't yours looks exactly like one that doesn't exist. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        sessionService.revoke(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /** "Log out other devices". */
    @PostMapping("/revoke-others")
    public RevokedCountResponse revokeOthers(@AuthenticationPrincipal UserPrincipal principal) {
        return new RevokedCountResponse(
                sessionService.revokeOthers(principal.getId(), principal.getSessionId()));
    }
}