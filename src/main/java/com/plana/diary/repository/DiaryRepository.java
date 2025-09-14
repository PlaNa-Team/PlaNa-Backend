package com.plana.diary.repository;

import com.plana.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// 제네릭 타입 2개가 들어간다. Diary는 엔티티 타입, Long은 Diary의 기본키 타입
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    // 작성자 + 날짜범위 필터
    @Query("SELECT d FROM Diary d " +
            "WHERE d.writer.id = :writerId " + // d.writer.id : DB에서 가져온 다이어리의 작성자 id, :writerId : 내가 찾고 싶은 작성자 id
            "And d.diaryDate BETWEEN :start AND :end " +
            "ORDER BY d.diaryDate ASC"
    )
    List<Diary> findMonthlyDiaries(@Param("writerId") Long writerId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    Optional<Diary> findTopByWriter_IdAndDiaryDateOrderByCreatedAtDesc(Long writerId, LocalDate diaryDate);

    @Query(value = """
        select d.*
        from (
          select
            d.id,
        
            -- 화면/조회 기준 날짜는 항상 일기 날짜
            date(d.diary_date) as display_date,
        
            -- 대표 선택 기준 타임스탬프
            --  - 내 글: updated_at 있으면 그걸, 없으면 created_at
            --  - 공유(내가 수락): accepted_at
            case
              when t.id is not null
                then t.accepted_at
              else coalesce(d.updated_at, d.created_at)
            end as activity_ts,
        
            row_number() over (
              order by
                case
                  when t.id is not null
                    then t.accepted_at
                  else coalesce(d.updated_at, d.created_at)
                end desc,
                d.id desc
            ) as rn
        
          from diary d
          left join diary_tag t
            on t.diary_id   = d.id
           and t.member_id  = :viewerId
           and t.tag_status = 'ACCEPTED'
        
          -- 내가 쓴 글 또는 내가 수락한 공유 글만 접근 가능
          where (d.member_id = :viewerId or t.id is not null)
        ) x
        join diary d on d.id = x.id
        where x.display_date = :date
          and x.rn = 1
        """, nativeQuery = true)
    Optional<Diary> findRepresentativeByDate(@Param("viewerId") Long viewerId,
                                             @Param("date") LocalDate date);




    @Query(value = """
/* Monthly 대표 일기 조회
   - 표시/조회 기준 날짜: 항상 diary.diary_date (달력용)
   - 대표 선정 기준(activity_ts):
       · 내 글        : COALESCE(d.updated_at, d.created_at)
       · 공유 수락 글 : t.accepted_at (viewer가 ACCEPTED한 태그만)
   - 하루(diary_date) 당 activity_ts가 가장 최신인 일기를 대표로 선택
*/
select d.*
from (
  select
    d.id,

    -- 달력/조회 기준 날짜: 무조건 diary_date
    date(d.diary_date) as display_date,

    -- 대표 선정용 비교 타임스탬프
    case
      when t.id is not null
        then t.accepted_at                     -- 공유(수락) 글
      else coalesce(d.updated_at, d.created_at) -- 내 글
    end as activity_ts,

    -- 하루(diary_date) 파티션 내에서
    --   1) activity_ts 최신 우선
    --   2) 동률이면 id 내림차순
    -- → rn=1 이 그 날의 대표 일기
    row_number() over (
      partition by date(d.diary_date)
      order by
        case
          when t.id is not null
            then t.accepted_at
          else coalesce(d.updated_at, d.created_at)
        end desc,
        d.id desc
    ) as rn

  from diary d
  -- viewer가 ACCEPTED한 태그만 조인 → t.*가 존재하면 '공유 수락 글'
  left join diary_tag t
    on t.diary_id   = d.id
   and t.member_id  = :viewerId
   and t.tag_status = 'ACCEPTED'

  -- 접근 가능 조건: 내 글 또는 내가 수락한 공유 글
  where (d.member_id = :viewerId or t.id is not null)
) x
-- 대표로 선정된 id를 다시 diary에 조인해 엔티티 매핑 안전하게
join diary d on d.id = x.id

-- 조회 기간: diary_date(=display_date) 기준
where x.display_date between :start and :end
  and x.rn = 1

-- 달력 정렬: 날짜 오름차순
order by x.display_date asc
""", nativeQuery = true)
    List<Diary> findMonthlyRepresentatives(@Param("viewerId") Long viewerId,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);



}
