package com.plana.lock.controller;

import com.plana.auth.dto.AuthenticatedMemberDto;
import com.plana.lock.service.LockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/locks/diaries")
@RequiredArgsConstructor
public class LockController {

    private final LockService lockService;

    /**
     * 락 획득
     * - 성공: token 반환
     * - 실패: 423 LOCKED + 현재 ownerId/TTL 정보 반환
     */
    @PostMapping("/{diaryId}/acquire")
    public ResponseEntity<?> acquire(@PathVariable Long diaryId,
                                     @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        String token = lockService.tryAcquire(diaryId, authMember.getId());
        if (token == null) {
            String holder = lockService.currentToken(diaryId); // "ownerId:UUID"
            long ttl = lockService.ttl(diaryId);
            Long ownerId = parseOwnerId(holder);
            return ResponseEntity.status(423) // LOCKED
                    .header("Retry-After", String.valueOf(Math.max(ttl, 0)))
                    .body(Map.of(
                            "acquired", false,
                            "ownerId", ownerId,
                            "ttlSeconds", Math.max(ttl, 0),
                            "expiresAt", Instant.now().plusSeconds(Math.max(ttl, 0)).toString()
                    ));
        }
        return ResponseEntity.ok(Map.of(
                "acquired", true,
                "token", token,
                "ownerId", authMember.getId(),
                "ttlSeconds", lockService.ttlSeconds(),
                "expiresAt", lockService.expiresAtFromNow().toString()
        ));
    }

    /**
     * 락 갱신
     */
    @PostMapping("/{diaryId}/renew")
    public ResponseEntity<?> renew(@PathVariable Long diaryId,
                                   @RequestHeader("X-Lock-Token") String token) {
        boolean ok = lockService.renew(diaryId, token);
        if (!ok) return ResponseEntity.status(423).body(Map.of("acquired", false));
        return ResponseEntity.ok(Map.of(
                "acquired", true,
                "ttlSeconds", lockService.ttlSeconds(),
                "expiresAt", lockService.expiresAtFromNow().toString()
        ));
    }

    /**
     * 락 해제
     */
    @PostMapping("/{diaryId}/release")
    public ResponseEntity<?> release(@PathVariable Long diaryId,
                                     @RequestHeader("X-Lock-Token") String token) {
        lockService.release(diaryId, token); // 멱등
        return ResponseEntity.noContent().build();
    }

    private Long parseOwnerId(String token) {
        if (token == null) return null;
        int idx = token.indexOf(':');
        try {
            return idx > 0 ? Long.parseLong(token.substring(0, idx)) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
