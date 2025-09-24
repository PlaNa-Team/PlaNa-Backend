package com.plana.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 세션 관리 서비스
 *
 * 사용자별 WebSocket 연결을 추적하고 관리
 * - 온라인 사용자 목록 관리
 * - 세션 ID와 Member ID 매핑
 * - 멀티 세션 지원 (한 사용자가 여러 디바이스/탭에서 접속)
 */
@Slf4j
@Service
public class WebSocketSessionManager {

    // memberId -> Set<sessionId> (한 사용자가 여러 세션을 가질 수 있음)
    private final Map<Long, Set<String>> memberSessions = new ConcurrentHashMap<>();

    // sessionId -> memberId (빠른 역조회용)
    private final Map<String, Long> sessionMembers = new ConcurrentHashMap<>();

    /**
     * 사용자 연결 등록
     *
     * @param memberId 사용자 ID
     * @param sessionId WebSocket 세션 ID
     */
    public void addUserSession(Long memberId, String sessionId) {
        // 기존 세션이 있으면 제거 (같은 sessionId로 재연결된 경우)
        removeSession(sessionId);

        // 새로운 세션 등록
        memberSessions.computeIfAbsent(memberId, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        sessionMembers.put(sessionId, memberId);

        log.info("사용자 세션 등록: memberId={}, sessionId={}, 총 세션 수={}",
                memberId, sessionId, memberSessions.get(memberId).size());
    }

    /**
     * 세션 해제
     *
     * @param sessionId 해제할 세션 ID
     */
    public void removeSession(String sessionId) {
        Long memberId = sessionMembers.remove(sessionId);
        if (memberId != null) {
            Set<String> sessions = memberSessions.get(memberId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    memberSessions.remove(memberId);
                    log.info("사용자 완전 오프라인: memberId={}", memberId);
                } else {
                    log.info("사용자 세션 해제: memberId={}, sessionId={}, 남은 세션 수={}",
                            memberId, sessionId, sessions.size());
                }
            }
        }
    }

    /**
     * 사용자 온라인 상태 확인
     *
     * @param memberId 확인할 사용자 ID
     * @return 온라인 여부
     */
    public boolean isUserOnline(Long memberId) {
        Set<String> sessions = memberSessions.get(memberId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * 사용자의 활성 세션 수 조회
     *
     * @param memberId 사용자 ID
     * @return 활성 세션 수
     */
    public int getUserSessionCount(Long memberId) {
        Set<String> sessions = memberSessions.get(memberId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * 사용자의 모든 세션 ID 조회
     *
     * @param memberId 사용자 ID
     * @return 세션 ID 집합 (없으면 빈 Set)
     */
    public Set<String> getUserSessions(Long memberId) {
        return memberSessions.getOrDefault(memberId, Set.of());
    }

    /**
     * 세션으로 사용자 ID 조회
     *
     * @param sessionId 세션 ID
     * @return 사용자 ID (없으면 null)
     */
    public Long getMemberIdBySession(String sessionId) {
        return sessionMembers.get(sessionId);
    }

    /**
     * 현재 온라인 사용자 수 조회
     *
     * @return 온라인 사용자 수
     */
    public int getOnlineUserCount() {
        return memberSessions.size();
    }

    /**
     * 전체 활성 세션 수 조회
     *
     * @return 전체 세션 수
     */
    public int getTotalSessionCount() {
        return sessionMembers.size();
    }

    /**
     * 현재 온라인 사용자 ID 목록 조회
     *
     * @return 온라인 사용자 ID 집합
     */
    public Set<Long> getOnlineUsers() {
        return Set.copyOf(memberSessions.keySet());
    }

    /**
     * 세션 정보 로깅 (디버깅용)
     */
    public void logSessionInfo() {
        log.info("=== WebSocket 세션 현황 ===");
        log.info("온라인 사용자 수: {}", getOnlineUserCount());
        log.info("전체 세션 수: {}", getTotalSessionCount());

        memberSessions.forEach((memberId, sessions) ->
                log.info("사용자 {}: {}개 세션 - {}", memberId, sessions.size(), sessions)
        );
    }
}