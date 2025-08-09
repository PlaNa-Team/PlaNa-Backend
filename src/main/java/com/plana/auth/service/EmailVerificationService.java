package com.plana.auth.service;

import com.plana.auth.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final MemberRepository memberRepository;
    private final StringRedisTemplate redis;
    private final EmailSenderService emailSender;

    private static final SecureRandom RAND = new SecureRandom();
    private static final Duration TTL = Duration.ofMinutes(3); // 인증번호 유효 시간
    private static final Duration COOLDOWN = Duration.ofSeconds(60); // 재발송 쿨다운 시간

    private String codeKey(String email) { return "email:verify:code:" + email; }
    private String throttleKey(String email) { return "email:verify:throttle:" + email; }

    @Transactional
    public boolean sendCodeIfNotDuplicated(String email) {
        // 1) 중복 체크
        if (memberRepository.existsByEmail(email)) {
            return true; // 중복
        }

        // 2) 쿨다운(스팸 방지)
        if (Boolean.TRUE.equals(redis.hasKey(throttleKey(email)))) {
            // 쿨다운 중이면 그냥 200 OK로 처리하거나, 429로 응답하도록 컨트롤러에서 분기 가능
            // 여기서는 조용히 무시하고 "보냈다" 메시지만 주고 끝내도 됨.
        } else {
            // 3) 코드 생성 및 저장
            String code = generateCode();
            redis.opsForValue().set(codeKey(email), code, TTL);
            redis.opsForValue().set(throttleKey(email), "1", COOLDOWN);

            // 4) 메일 발송
            String subject = "[PlaNa] 이메일 인증번호 안내";
            String body = """
                    안녕하세요.
                    아래 인증번호를 입력해 이메일 인증을 완료해주세요.

                    인증번호: %s
                    유효시간: %d분
                    """.formatted(code, TTL.toMinutes());
            emailSender.send(email, subject, body);
        }

        return false; // 미중복 + 발송 처리
    }

    private String generateCode() {
        int n = RAND.nextInt(1_000_000);    // 0 ~ 999999
        return String.format("%06d", n);    // 앞자리가 0이어도 6자리 유지
    }
}
