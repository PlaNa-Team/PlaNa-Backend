
// 🔥 완전한 순수 WebSocket 연결 스크립트 (라이브러리 포함)
window.testPureWebSocket = async function() {
    console.clear();
    console.log('🔥 순수 WebSocket 연결 테스트 (라이브러리 자동 로딩)');

    // 기존 연결 정리
    if (window.stompClient?.connected) {
        window.stompClient.disconnect();
    }

    // 1. Stomp 라이브러리 로딩 (SockJS는 필요 없음)
    if (typeof Stomp === 'undefined') {
        console.log('📦 Stomp 라이브러리 로딩 중...');
        await new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = 'https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js';
            script.onload = resolve;
            script.onerror = reject;
            document.head.appendChild(script);
        });
        console.log('✅ Stomp 로딩 완료');
    }

    // 2. 토큰 확인
    const token = localStorage.getItem('accessToken');
    if (!token) {
        console.error('❌ accessToken 없음');
        return;
    }
    console.log('🔑 토큰 확인 완료:', token.length + '자');

    // 3. 순수 WebSocket 연결
    const wsUrl = `wss://plana.hoonee-math.info/api/ws?token=${encodeURIComponent(token)}`;
    console.log('🔌 WebSocket 연결 중:', wsUrl);

    return new Promise((resolve, reject) => {
        // 순수 WebSocket 생성
        const socket = new WebSocket(wsUrl);
        window.stompClient = Stomp.over(socket);

        // 디버그 설정
        window.stompClient.debug = function(str) {
            if (str.includes('CONNECTED') || str.includes('ERROR') || str.includes('MESSAGE')) {
                console.log('🔧 STOMP:', str);
            }
        };

        // WebSocket 이벤트 핸들러
        socket.onopen = function() {
            console.log('🟢 WebSocket 연결 열림');
        };

        socket.onerror = function(error) {
            console.error('❌ WebSocket 에러:', error);
        };

        socket.onclose = function(event) {
            console.log('🔴 WebSocket 닫힘:', event.code, event.reason);
        };

        // STOMP 연결
        window.stompClient.connect(
            { 'Authorization': 'Bearer ' + token },
            function(frame) {
                console.log('✅ STOMP 연결 성공!');

                // 🎯 모든 가능한 경로로 구독
                const subscriptionPaths = [
                    '/user/queue/notifications',      // Spring 표준
                    '/user/24/queue/notifications',   // 직접 경로
                    '/topic/notifications',           // 브로드캐스트
                    '/queue/notifications'            // 단순 경로
                ];

                subscriptionPaths.forEach((path, index) => {
                    const subscription = window.stompClient.subscribe(path, function(message) {
                        console.log(`🎯 [${path}] 메시지 수신!`);
                        console.log('📦 Headers:', message.headers);
                        console.log('📦 Body:', message.body);

                        try {
                            const data = JSON.parse(message.body);
                            console.log(`🎯 [${path}] 파싱된 데이터:`, data);
                        } catch (e) {
                            console.log(`🎯 [${path}] 원본 메시지:`, message.body);
                        }
                    }, { id: `pure-sub-${index}` });

                    console.log(`📫 구독 완료: ${path} (ID: pure-sub-${index})`);
                });

                // 세션 등록
                window.stompClient.send("/app/connect", {}, "{}");
                console.log('📤 세션 등록 완료');

                resolve();
            },
            function(error) {
                console.error('❌ STOMP 연결 실패:', error);
                reject(error);
            }
        );

        // 타임아웃 (15초)
        setTimeout(() => {
            if (!window.stompClient?.connected) {
                reject(new Error('연결 타임아웃'));
            }
        }, 15000);
    });
};

// 🧪 테스트 메시지 발송 함수
window.sendTestMessage = async function() {
    try {
        const response = await fetch('https://plana.hoonee-math.info/api/notifications/test-message', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('accessToken')
            }
        });
        const result = await response.json();
        console.log('📤 테스트 메시지 발송 결과:', result);
    } catch (error) {
        console.error('테스트 메시지 발송 실패:', error);
    }
};

// 📊 연결 상태 확인 함수
window.checkConnection = function() {
    console.log('📊 연결 상태:', {
        WebSocket연결됨: window.stompClient?.ws?.readyState === 1,
        STOMP연결됨: window.stompClient?.connected || false,
        구독수: window.stompClient ? Object.keys(window.stompClient.subscriptions).length : 0,
        구독목록: window.stompClient ? Object.keys(window.stompClient.subscriptions) : []
    });
};

// 🚀 자동 실행
console.log('🎯 순수 WebSocket 함수 준비 완료!');
console.log('사용법:');
console.log('  testPureWebSocket()  - 순수 WebSocket 연결');
console.log('  sendTestMessage()    - 테스트 메시지 발송');
console.log('  checkConnection()    - 연결 상태 확인');
console.log('');
console.log('자동 연결 시작...');

// 자동으로 연결 시도
testPureWebSocket().then(() => {
    console.log('🎉 순수 WebSocket 연결 성공!');
    console.log('이제 다음 방법으로 테스트하세요:');
    console.log('1. sendTestMessage() - 수동 테스트');
    console.log('2. Postman으로 다른 계정에서 24번 태그');
    console.log('3. 10초마다 자동 메시지 확인');
}).catch(error => {
    console.error('자동 연결 실패:', error);
    console.log('순수 WebSocket도 실패했습니다. 서버나 보안 설정 문제일 수 있습니다.');
});

console.log('이 스크립트의 특징:');
console.log('1. ✅ SockJS 없이 순수 WebSocket만 사용');
console.log('2. ✅ 모든 가능한 경로 구독 - 4개 경로 동시 구독');
console.log('3. ✅ 자동 라이브러리 로딩 - Stomp.js만 필요');
console.log('4. ✅ 완전 자동화 - 붙여넣으면 바로 연결');
console.log('5. ✅ 상세 디버깅 - 모든 단계 로깅');
