package com.plana.auth.service;

import com.plana.auth.enums.VerificationPurpose;
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
    private String verifiedKey(String email) { return "email:verify:ok:" + email; }
    public enum VerifyResult { OK, EXPIRED, MISMATCH, NOT_FOUND }

    @Transactional
    public boolean sendCode(String rawEmail, VerificationPurpose purpose) {
        String email = rawEmail.trim().toLowerCase();

        boolean exists = memberRepository.existsByEmail(email);
        // 존재 조건 판정
        if (purpose.shouldExist() && !exists) return false;     // 있어야 하는데 없음
        if (!purpose.shouldExist() && exists) return false;     // 없어야 하는데 있음

        // 쿨다운 체크(있으면 조용히 패스해도 되고 429로 처리해도 됨)
        if (!Boolean.TRUE.equals(redis.hasKey(throttleKey(email)))) {
            String code = generateCode();
            redis.opsForValue().set(codeKey(email), code, TTL);
            redis.opsForValue().set(throttleKey(email), "1", COOLDOWN);

            String subject = switch (purpose) {
                case SIGN_UP -> "[PlaNa] 회원가입 이메일 인증번호 안내";
                case FIND_ID -> "[PlaNa] 아이디 찾기 이메일 인증번호 안내";
                case RESET_PASSWORD -> "[PlaNa] 비밀번호 재설정 인증번호 안내";
            };
            String body = """
                    안녕하세요.
                    아래 인증번호를 입력해 주세요.

                    인증번호: %s
                    유효시간: %d분
                    """.formatted(code, TTL.toMinutes());

            emailSender.send(email, subject, body);
        }
        return true; // 정책 조건 만족 & 발송 처리
    }

    private String generateCode() {
        int n = RAND.nextInt(1_000_000);    // 0 ~ 999999
        return String.format("%06d", n);    // 앞자리가 0이어도 6자리 유지
    }

    public VerifyResult verifyCode(String rawEmail, String inputCode) {
        String email = rawEmail.trim().toLowerCase();
        String key = codeKey(email);
        String saved = redis.opsForValue().get(key);
        if (saved == null) {
            // TTL 지나서 사라졌거나, 애초에 발급이 안 됨
            return VerifyResult.EXPIRED; // 또는 NOT_FOUND로 구분해도 됨
        }
        if (!saved.equals(inputCode)) {
            return VerifyResult.MISMATCH;
        }
        // 성공: 코드 제거 + "인증 완료" 플래그(예: 30분 유지)
        redis.delete(key);
        redis.opsForValue().set(verifiedKey(email), "true", Duration.ofMinutes(30)); // 30분동안 해당 이메일 인증 확인 했다는 표시를 가짐
        return VerifyResult.OK;
    }

    public boolean isVerified(String email) {
        String key = verifiedKey(email.trim().toLowerCase());
        return Boolean.TRUE.toString().equals(redis.opsForValue().get(key));
    }

    // 인증 만료
    public void invalidateVerified(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        redis.delete(verifiedKey(email));  // Redis에서 인증 OK 플래그 제거
    }
}
