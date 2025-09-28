package com.plana.lock.service;

import java.time.Instant;

public interface LockService {

    /** 락 획득 (성공 시 토큰 반환, 실패 시 null) */
    String tryAcquire(Long diaryId, Long ownerId);

    /** 남은 TTL(초). 없으면 0 또는 음수 */
    long ttl(Long diaryId);

    /** 현재 홀더 토큰 반환 (없으면 null) */
    String currentToken(Long diaryId);

    /** 토큰 소유자 여부 */
    boolean isOwner(Long diaryId, String token);

    /** 연장(소유자일 때만) */
    boolean renew(Long diaryId, String token);

    /** 해제(소유자일 때만) */
    boolean release(Long diaryId, String token);

    /** TTL 기준 만료 예상 시각 */
    Instant expiresAtFromNow();

    /** TTL(기본 설정 값) */
    long ttlSeconds();
}
