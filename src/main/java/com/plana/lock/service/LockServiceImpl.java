package com.plana.lock.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LockServiceImpl implements LockService{

    private final StringRedisTemplate redis;

    // 소유자 일치 시에만 DEL (체크-앤-딜리트)
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('del', KEYS[1]) " +
                    "else return 0 end",
            Long.class
    );

    private static final long TTL_SECONDS = 300; // 5분

    private String key(Long diaryId) {
        return "diary:lock:" + diaryId;
    }

    /** 락 획득 (성공 시 토큰 반환, 실패 시 null) */
    public String tryAcquire(Long diaryId, Long ownerId) {
        String token = ownerId + ":" + UUID.randomUUID();
        Boolean ok = redis.opsForValue()
                .setIfAbsent(key(diaryId), token, Duration.ofSeconds(TTL_SECONDS));
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /** 남은 TTL(초). 없으면 0 또는 음수 */
    public long ttl(Long diaryId) {
        Long ttl = redis.getExpire(key(diaryId), TimeUnit.SECONDS);
        return ttl == null ? 0 : ttl;
    }

    /** 현재 홀더 토큰 반환 (없으면 null) */
    public String currentToken(Long diaryId) {
        return redis.opsForValue().get(key(diaryId));
    }

    /** 토큰 소유자 여부 */
    public boolean isOwner(Long diaryId, String token) {
        String cur = currentToken(diaryId);
        return cur != null && cur.equals(token);
    }

    /** 연장(소유자일 때만) */
    public boolean renew(Long diaryId, String token) {
        if (!isOwner(diaryId, token)) return false;
        return Boolean.TRUE.equals(redis.expire(key(diaryId), Duration.ofSeconds(TTL_SECONDS)));
    }

    /** 해제(소유자일 때만) */
    public boolean release(Long diaryId, String token) {
        Long res = redis.execute(UNLOCK_SCRIPT, Collections.singletonList(key(diaryId)), token);
        return res != null && res == 1L;
    }

    public Instant expiresAtFromNow() {
        return Instant.now().plusSeconds(TTL_SECONDS);
    }

    public long ttlSeconds() { return TTL_SECONDS; }
}
