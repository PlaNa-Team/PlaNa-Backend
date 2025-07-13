## JWT 활용 - 인증 받은 사용자 정보를 이용한 비즈니스 로직 접근 방법

아래 코드와 같이 @Param 에 들어가는 @AuthenticationPrincipal 은 Spring Security 가 제공하는 인증된 사용자 정보를 이용할 수 있게 해준다.

해당 정보는 JwtAuthenticationFilter.java 에서 설정해 주었음.

AuthenticatedMemberDto 를 이용해 인증 받은 사용자 정보에 필요한 필수 정보를 Member Entity 에서 추려서 DTO로 만들어줌.

member의 id 값이나 AuthenticatedMemberDto 내부 정보를 이용할 필요가 없을 경우에는
param 에 굳이 @AuthenticationPrincipal 을 넣어줄 필요는 없음.

인증받은 사용자의 member.id 라던지, member.email 등을 사용하고자 할때,
param 에 "@AuthenticationPrincipal AuthenticatedMemberDto authMember"을 추가한 후 아래와 같이
authMember.getId() 처럼 id값을 직접 이용할 수 있음

```java
    /**
     * 현재 로그인한 사용자 정보 조회 (JWT 기반)
     * 실제 프론트엔드에서 사용할 회원정보 API
     * @AuthenticationPrincipal AuthenticatedMemberDto Spring Security에서 인증된 사용자 정보
     * @return 사용자 정보 응답
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedMemberDto authMember) {
        
        if (authMember == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        // DB에서 최신 사용자 정보 조회 (토큰의 정보가 오래될 수 있음)
        Optional<Member> memberOptional = memberRepository.findById(authMember.getId());
        
        if (memberOptional.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        Member member = memberOptional.get();
        
        // 계정 비활성화 상태 확인
        if (!member.getEnabled()) {
            return ResponseEntity.status(403).body(Map.of("error", "Account is disabled"));
        }
        
        // 깔끔한 사용자 정보 응답
        Map<String, Object> response = new HashMap<>();
        response.put("id", member.getId());
        response.put("email", member.getEmail());
        response.put("name", member.getName());
        response.put("profileImageUrl", member.getProfileImageUrl());
        response.put("provider", member.getProvider().getValue());
        response.put("role", member.getRole());
        response.put("enabled", member.getEnabled());
        response.put("createdAt", member.getCreatedAt());
        response.put("updatedAt", member.getUpdatedAt());
        
        log.info("User info requested via JWT: {}", member.getEmail());
        return ResponseEntity.ok(response);
    }

```