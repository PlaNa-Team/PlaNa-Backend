# 프로덕션 환경 WebSocket 네트워크 문제 해결 가이드

## 📋 **문제 개요**

로컬에서는 정상 작동하던 WebSocket이 프로덕션 환경(Docker + nginx proxy manager)에서 연결되지 않는 문제가 발생했습니다.

### **환경 정보**
- **프론트엔드**: https://plana-frontend-silk.vercel.app (HTTPS)
- **백엔드**: https://plana.hoonee-math.info (nginx proxy manager + Docker)
- **WebSocket**: Spring Boot STOMP WebSocket
- **프록시**: nginx proxy manager (Docker 컨테이너)

## 🚨 **발생한 문제들과 해결 과정**

### **1단계: HTTPS Mixed Content 문제**

#### **문제**
```
Mixed Content: The page at 'https://plana-frontend-silk.vercel.app/calendar'
was loaded over HTTPS, but attempted to connect to the insecure WebSocket
endpoint 'ws://plana.hoonee-math.info/api/ws'
```

#### **원인**
- 프론트엔드: HTTPS
- WebSocket: HTTP (ws://)
- 브라우저 보안정책으로 Mixed Content 차단

#### **해결**
```javascript
// ❌ 문제
const wsUrl = `ws://plana.hoonee-math.info/api/ws?token=${token}`;

// ✅ 해결
const wsUrl = `wss://plana.hoonee-math.info/api/ws?token=${token}`;
```

### **2단계: Stomp.js 라이브러리 Mixed Content**

#### **문제**
```
Mixed Content: The page at 'https://plana-frontend-silk.vercel.app/calendar'
was loaded over HTTPS, but requested an insecure script
'http://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js'
```

#### **해결**
```javascript
// ❌ 문제
script.src = 'http://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js';

// ✅ 해결
script.src = 'https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js';
```

### **3단계: nginx WebSocket 지원 설정**

#### **문제**
nginx가 WebSocket Upgrade 헤더를 제대로 전달하지 못함

#### **해결**
nginx proxy manager Custom Location (`/api/ws`)에 다음 추가:
```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_read_timeout 86400;
```

### **4단계: Docker 네트워크 문제 (핵심 해결)**

#### **문제**
Spring Boot 서버 로그에서 WebSocket 연결 시도가 전혀 감지되지 않음:
```
WebSocketSession[0 current WS(0), 0 total, 0 closed abnormally]
stompSubProtocol[processed CONNECT(0)-CONNECTED(0)-DISCONNECT(0)]
```

#### **근본 원인**
nginx proxy manager가 생성한 설정 파일에서 잘못된 proxy_pass 주소:

```nginx
# ❌ 잘못된 설정 (nginx 컨테이너 내부 localhost)
location /api/ws {
    proxy_pass http://127.0.0.1:8080;
    # ...
}

# ❌ 일반 API는 올바른 설정
location /api/ {
    proxy_pass http://planner-backend:8080;
    # ...
}
```

#### **문제 분석**
- `127.0.0.1:8080`: nginx 컨테이너 **내부**의 localhost
- `planner-backend:8080`: Docker **네트워크** 상의 Spring Boot 컨테이너
- nginx 컨테이너에는 8080 포트에서 실행 중인 서비스가 없음
- 따라서 WebSocket 요청이 Spring Boot에 도달하지 못함

#### **해결**
```bash
# 서버에서 직접 수정
sed -i 's|proxy_pass http://127.0.0.1:8080|proxy_pass http://planner-backend:8080|' /home/hooneeubuntu/proxy/data/nginx/proxy_host/2.conf
docker-compose restart nginx-proxy-manager
```

또는 nginx proxy manager UI에서:
- **Custom Location**: `/api/ws`
- **Forward Hostname/IP**: `planner-backend` (127.0.0.1 아님!)
- **Forward Port**: `8080`

## 🎯 **최종 해결 방법**

### **nginx proxy manager 설정**

#### **Proxy Host 기본 설정**
- **Domain**: plana.hoonee-math.info
- **Forward Hostname/IP**: planner-frontend_react_nginx-1
- **Forward Port**: 80
- **SSL**: Let's Encrypt 활성화

#### **Custom Locations**

**1. 일반 API 설정 (`/api/`)**
- **Forward Hostname/IP**: planner-backend
- **Forward Port**: 8080
- **Scheme**: http

**2. WebSocket 설정 (`/api/ws`)**
- **Forward Hostname/IP**: planner-backend ⚠️ **중요: 127.0.0.1 사용 금지**
- **Forward Port**: 8080
- **Scheme**: http
- **Advanced 탭**:
```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_read_timeout 86400;
```

### **프론트엔드 연결 코드**

```javascript
// WSS 연결 (HTTPS 호환)
const wsUrl = `wss://plana.hoonee-math.info/api/ws?token=${encodeURIComponent(token)}`;
const socket = new WebSocket(wsUrl);
const stompClient = Stomp.over(socket);

// HTTPS CDN 사용
const script = document.createElement('script');
script.src = 'https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js';
```

## 🔧 **디버깅 방법**

### **1. Spring Boot 로그 모니터링**
```bash
ssh hoonee "cd /var/team-workspace/genius/planner && docker logs -f planner-backend"
```

**정상 연결 시 로그:**
```
WebSocket 핸드셰이크 시작: /api/ws?token=...
WebSocket 핸드셰이크 인증 성공: memberId=24, email=...
WebSocketSession[1 current WS(1), 1 total...]
```

### **2. nginx 설정 파일 확인**
```bash
ssh hoonee "cat /home/hooneeubuntu/proxy/data/nginx/proxy_host/2.conf | grep -A 5 '/api/ws'"
```

**올바른 설정:**
```nginx
location /api/ws {
    proxy_pass http://planner-backend:8080;  # ✅ 컨테이너 이름 사용
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

### **3. Docker 네트워크 확인**
```bash
ssh hoonee "cd /var/team-workspace/genius/planner && docker-compose ps"
ssh hoonee "cd /home/hooneeubuntu/proxy && docker network inspect proxy_default"
```

## ⚠️ **주의사항**

### **1. Docker 네트워크 이해**
- `127.0.0.1`: 각 컨테이너 **내부**의 localhost
- `컨테이너이름:포트`: Docker **네트워크** 상의 다른 컨테이너
- nginx proxy manager에서는 항상 **컨테이너 이름** 사용

### **2. 혼재된 설정 방지**
- SockJS와 순수 WebSocket을 섞지 말 것
- 일관된 프록시 설정 유지

### **3. Mixed Content 보안정책**
- HTTPS 페이지에서는 반드시 WSS 사용
- 모든 외부 리소스(CDN 등)도 HTTPS 사용

## 📊 **성능 최적화**

### **WebSocket 연결 설정**
```nginx
proxy_read_timeout 86400;        # 24시간 연결 유지
proxy_connect_timeout 60s;       # 연결 타임아웃
proxy_send_timeout 60s;          # 전송 타임아웃
```

### **Connection Pool 설정**
```yaml
# application.yml
spring:
  websocket:
    max-text-message-buffer-size: 32768
    max-binary-message-buffer-size: 32768
    max-session-idle-timeout: 600000
```

## 🎉 **최종 결과**

모든 설정 완료 후:
- ✅ WebSocket WSS 연결 성공
- ✅ 실시간 알림 정상 작동
- ✅ Spring Boot 로그에서 연결 확인:
```
WebSocketSession[1 current WS(1)-HttpStream(0)-HttpPoll(0), 1 total...]
stompSubProtocol[processed CONNECT(1)-CONNECTED(1)-DISCONNECT(0)]
```

## 📖 **교훈**

1. **로컬과 프로덕션 차이**: Docker 네트워크는 별도의 주소 체계
2. **nginx proxy manager 함정**: UI와 실제 설정 파일 불일치 가능성
3. **단계적 디버깅**: 브라우저 → nginx → Spring Boot 순서로 추적
4. **Mixed Content**: HTTPS 환경에서는 모든 리소스를 HTTPS로 통일

---

**작성일**: 2025-09-24
**해결 소요 시간**: 약 2시간
**핵심 해결책**: Docker 네트워크 이해와 올바른 컨테이너 이름 사용