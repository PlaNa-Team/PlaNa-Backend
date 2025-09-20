# 이번 작업 목적! NotificationController 작업을 목적으로 한다.
- NotificationController 는 웹소켓을 이용해 사용자가 로그인하면 웹소켓을 연결하고, 알림을 보내준다.
- 알림은 '다이어리 친구 태그 알림', '스케줄에 사용자가 직접 입력한 알람 계획에 맞춰 정해진 시점에 알려주는 알림' 이렇게 총 두가지 알림이 존재한다.
- 처음 계획활 때는 이 두가지를 한 번에 관리하고자 '통합 알림(notification entity)' 테이블을 만들었다. (../entity/Notification.java)
- 기능을 개발하기에 앞서 'Diary 및 DiaryTag 등록', 'Schedule 및 ScheduleAlarm 등록' 기능이 잘 작동하는지 로직을 분석하고 분석 보고서를 작성해야한다.
- 각 분석 보고서를 작성할 때 중점적으로 확인해야할 사항 및 '통합 알림' 기능의 핵심은 다음과 같다.

1. Diary
    1. 다이어리를 등록할 때, 사용자는 태그를 달 수 있다. 직접 입력한 태그가 들어가는 경우는 알람이 뜨지 않는다. 친구(사용자)들을 함께 태그하여 다이어리를 등록할때 notification 테이블에도 태그된 각 사용자마다 알림이 가도록 추가되어야 한다.
    2. 서비스를 사용중인 사람에게도 알림이 가야하며, 사용중이지 않던 사람은 로그인 했을 때, 또는 앱에 알림이 오는 식으로 알림이 가야한다.
    3. 이때 친구가 받은 알림에는 본인이 작성한 diary에 대한 기본정보(diary.id, diary.date, member.id(diary_tags.tag_status == '작성자'), member.login_id, member.name 등의 정보)를 포함해야한다. 친구가 함께 태그하는 것을 수락할 경우 diary_tags 테이블의 해당 다이어리의 tag_status 값을 변경해줄 수 있기 때문이다.
    
2. Schedule
    1. Schedule을 등록할 때, schedule_alarm 도 함께 등록된다.
    2. 각 schedule_alarm의 데이터는 schedule.start_at 을 기준으로 'schedule_alarm.notify_before_val'+'schedule_alarm.notify_unit'전에 울려야 한다. 즉 '10'+'분'전에 울리거나 1시간 전, 1주일 전 등 하나의 스케줄에 여러개의 알람이 등록되어있을 수 있다. 
    3. schedule 과 schedule_alarm 에 의해 생성되는 통합알림은 schedule.recurrence_rule 및 schedule.recurrence_until 을 고려해야한다.
    4. 스케줄 알림이 보내질 때 schedule 에 대한 기본정보를 포함해야 한다.

- 위와 같이 두 알림기능을 notification entity와 그 컨트롤러로 통합관리할 수 있는지 우선 분석한 후 ./Plan.md(만들어져있음) 파일에 보고서 및 기능 개발 계획을 작성한다
- 

# 관련 entity
- 다음 entity는 정확히 분석하자. 관련 controller 나 비즈니스 로직도 필요하면 꼭 분석하자.
- notification 이외의 controller를 작업해야할 때는 이미 구현된 기능이 있는지 확인하고 최대한 기존 기능을 이용하도록 하자.
- com.plana.calendar.entity.Schedule
- com.plana.calendar.entity.ScheduleAlarm
- com.plana.diary.entity.Diary
- com.plana.diary.entity.DiaryTag
- com.plana.notification.entity.Notification

# 관련 API 명세서
- 다음 두 api 는 웹소캣을 사용할지 정확하게 모르는 상태에서 임시로 만들어뒀던 api 예시이다. 참고만하자.
- 웨소캣을 사용할 경우 프론트에서 get 요청을 보내는것이 맞는지 잘 모르겠다. get 요청이 필요한가?
- 실시간 알림이 가능하도록 구현하는 것을 목표로 하고 다음 api dPt

## Request

```
GET /api/notifications?page=1&size=20&unreadOnly=true
```
## Response

```
{
  "status": 200,
  "body": {
    "data": [
      {
        "id": 1, // 이건 통합알림 고유번호
        "type": "ALARM",
        "message": "10분 후 '프로젝트 회의'가 시작됩니다",
        "time": "2025-06-28T14:50:00", // 통합 알림 보내는 시간
        "isRead": false, // 통합 알림 읽음 여부
        "createdAt": "2025-06-28T14:50:00",
        "relatedData": { // type 에 따라서 저장
          "scheduleId": 123 // 스케줄 알람일 경우 어떤 id로 저장할까
          // "alarmId"
        }
      },
      {
        "id": 2,
        "type": "TAG",
        "message": "광훈님이 다이어리에 회원님을 태그했습니다",
        "time": "2025-06-28T12:30:00",
        "isRead": false,
        "createdAt": "2025-06-28T12:30:00",
        "relatedData": {
          "diaryId": 23
        }
      }
    ],
    "pagination": {
      "currentPage": 1,
      "totalPages": 3,
      "totalCount": 45,
      "unreadCount": 8
    }
  }
}
```
