package com.plana.calendar.repository;

import com.plana.calendar.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 메모 엔티티에 대한 데이터 액세스 인터페이스
 */
public interface MemoRepository extends JpaRepository<Memo, Long> {
    
    /**
     * 특정 연도, 주차 범위에 해당하는 메모 목록 조회 (타입별)
     * ISO 주차 번호 기준으로 범위 내 메모를 조회
     * 
     * @param memberId 사용자 ID
     * @param year 조회할 연도
     * @param startWeek 시작 주차
     * @param endWeek 종료 주차  
     * @param type 메모 타입 (다이어리/스케줄)
     * @return 해당 범위의 메모 목록
     */
    @Query("SELECT m FROM Memo m " +
            "WHERE m.member.id = :memberId " +
            "AND m.year = :year " +
            "AND m.week BETWEEN :startWeek AND :endWeek " +
            "AND m.type = :type " +
            "ORDER BY m.week ASC, m.createdAt ASC")
    List<Memo> findMemosInWeekRange(@Param("memberId") Long memberId,
                                   @Param("year") Short year,
                                   @Param("startWeek") Short startWeek,
                                   @Param("endWeek") Short endWeek,
                                   @Param("type") Memo.MemoType type);
    
    /**
     * 사용자가 소유한 메모 조회 (권한 확인용)
     * 
     * @param id 메모 ID
     * @param memberId 사용자 ID
     * @return 메모 엔티티 (Optional)
     */
    @Query("SELECT m FROM Memo m " +
            "WHERE m.id = :id " +
            "AND m.member.id = :memberId")
    Optional<Memo> findByIdAndMemberId(@Param("id") Long id, 
                                      @Param("memberId") Long memberId);
    
    /**
     * 특정 사용자-타입-연도-주차의 메모 개수 확인 (중복 방지용)
     * 성능 최적화: 전체 엔티티 조회 대신 COUNT만 반환
     * 
     * @param memberId 사용자 ID
     * @param year 연도
     * @param week 주차
     * @param type 메모 타입
     * @return 해당 조건의 메모 개수
     */
    @Query("SELECT COUNT(m) FROM Memo m " +
            "WHERE m.member.id = :memberId " +
            "AND m.year = :year " +
            "AND m.week = :week " +
            "AND m.type = :type")
    int countExistingMemos(@Param("memberId") Long memberId,
                          @Param("year") Short year,
                          @Param("week") Short week,
                          @Param("type") Memo.MemoType type);

}