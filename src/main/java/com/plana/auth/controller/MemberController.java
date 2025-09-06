package com.plana.auth.controller;

import com.plana.auth.dto.AuthenticatedMemberDto;
import com.plana.auth.dto.MemberInfoResponseDto;
import com.plana.auth.dto.NicknameUpdateRequestDto;
import com.plana.auth.exception.UnauthorizedException;
import com.plana.auth.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("")
    public ResponseEntity<?> getMyInfo(
            @AuthenticationPrincipal AuthenticatedMemberDto auth
    ) {
        if (auth == null) throw new UnauthorizedException("인증이 필요합니다");

        MemberInfoResponseDto data = memberService.getMyInfo(auth.getId());
        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "회원 정보 조회를 성공했습니다.",
                "data", data
        ));
    }

    // 아이디 중복 확인
    @GetMapping("/check-id")
    public ResponseEntity<Map<String, Object>> checkLoginId(@RequestParam String loginId) {

        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }

        boolean available = memberService.isLoginIdExists(loginId);

        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.",
                "status", 200
        ));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteMe(
            @AuthenticationPrincipal AuthenticatedMemberDto auth,
            HttpServletResponse response) {

        if (auth == null) throw new UnauthorizedException("인증이 필요합니다");

        memberService.deleteMe(auth.getId());

        // 리프레시 토큰 쿠키 제거
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // prod는 true
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "회원 탈퇴가 완료되었습니다.",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @PutMapping("/nickname")
    public ResponseEntity<?> updateNickname(
            @RequestBody NicknameUpdateRequestDto req,
            @AuthenticationPrincipal AuthenticatedMemberDto auth
    ) {
        if (auth == null) throw new UnauthorizedException("인증이 필요합니다");

        memberService.updateNickname(auth.getId(), req.getNickname());
        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "닉네임이 변경되었습니다.",
                "timestamp", System.currentTimeMillis()
        ));
    }

}

