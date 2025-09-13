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
        select *
        from (
          select
            d.*,
        
            -- 화면/조회에서 '해당 날짜'를 판별할 기준
            --   - 내 글: 일기 날짜(diary_date)
            --   - 공유(내가 수락한 글): 수락 시각(accepted_at)의 '날짜'
            date(
              case
                when t.member_id is not null and t.tag_status = 'ACCEPTED'
                  then t.accepted_at
                else d.diary_date
              end
            ) as display_date,
        
            --  '최신 여부'를 판정할 비교 타임스탬프
            --   - 내 글: 수정시각(없으면 생성시각)
            --   - 공유: 수락 시각
            (case
               when t.member_id is not null and t.tag_status = 'ACCEPTED'
                 then t.accepted_at
               else coalesce(d.updated_at, d.created_at)
             end) as activity_ts,
        
            row_number() over (
              order by
                --  우선순위 없이, 더 '최근에 발생한(activity_ts)' 것이 위로
                (case
                   when t.member_id is not null and t.tag_status = 'ACCEPTED'
                     then t.accepted_at
                   else coalesce(d.updated_at, d.created_at)
                 end) desc,
                d.id desc
            ) rn
        
          from diary d
          left join diary_tag t
            on t.diary_id   = d.id
           and t.member_id  = :viewerId
           and t.tag_status = 'ACCEPTED'
        
          -- 내가 쓴 글 또는 내가 수락한 공유 글만 접근 가능
          where (d.member_id = :viewerId or t.id is not null)
        ) x
        where x.display_date = :date
          and x.rn = 1
        """, nativeQuery = true)
    Optional<Diary> findRepresentativeByDate(@Param("viewerId") Long viewerId,
                                             @Param("date") LocalDate date);



    @Query(value = """
        select *
        from (
          select
            d.*,
            date(
              case
                when t.member_id is not null and t.tag_status = 'ACCEPTED'
                  then t.accepted_at
                else coalesce(d.updated_at, d.created_at)
              end
            ) as display_date,
            (case
               when t.member_id is not null and t.tag_status = 'ACCEPTED'
                 then t.accepted_at
               else coalesce(d.updated_at, d.created_at)
             end) as activity_ts,
            row_number() over (
              partition by
                date(
                  case
                    when t.member_id is not null and t.tag_status = 'ACCEPTED'
                      then t.accepted_at
                    else coalesce(d.updated_at, d.created_at)
                  end
                )
              order by
                (case
                   when t.member_id is not null and t.tag_status = 'ACCEPTED'
                     then t.accepted_at
                   else coalesce(d.updated_at, d.created_at)
                 end) desc,
                d.id desc
            ) rn
          from diary d
          left join diary_tag t
            on t.diary_id   = d.id
           and t.member_id  = :viewerId
           and t.tag_status = 'ACCEPTED'
          where (d.member_id = :viewerId or t.id is not null)
        ) x
        where x.display_date between :start and :end
          and x.rn = 1
        order by x.display_date asc
        """, nativeQuery = true)
    List<Diary> findMonthlyRepresentatives(@Param("viewerId") Long viewerId,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);


}
