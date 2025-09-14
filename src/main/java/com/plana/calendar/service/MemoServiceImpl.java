package com.plana.calendar.service;

import com.plana.auth.entity.Member;
import com.plana.auth.repository.MemberRepository;
import com.plana.calendar.dto.request.MemoInsertRequestDto;
import com.plana.calendar.dto.request.MemoUpdateRequestDto;
import com.plana.calendar.dto.response.MemoDetailResponseDto;
import com.plana.calendar.dto.response.MemoMonthlyItemDto;
import com.plana.calendar.entity.Memo;
import com.plana.calendar.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 메모 서비스 구현체
 * ISO 표준 주차 번호 기반 메모 관리
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemoServiceImpl implements MemoService {
    
    private final MemoRepository memoRepository;
    private final MemberRepository memberRepository;
    
    @Override
    public List<MemoMonthlyItemDto> getMonthlyMemos(Long memberId, int year, int month, String type) {
        // 입력 검증
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month 값이 1~12 사이의 숫자여야 합니다.");
        }
        
        if (!isValidMemoType(type)) {
            throw new IllegalArgumentException("type 값이 '다이어리' 또는 '스케줄'이어야 합니다.");
        }
        
        // 해당 월의 주차 범위 계산 (ISO 표준)
        YearMonth yearMonth = YearMonth.of(year, month);
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        
        // 월의 첫 주와 마지막 주 계산
        int startWeek = yearMonth.atDay(1).get(weekFields.weekOfYear());
        int endWeek = yearMonth.atEndOfMonth().get(weekFields.weekOfYear());
        System.out.println("startWeek = " + startWeek+" / endWeek = " + endWeek);
        
        // 연도 경계 처리 (12월 마지막 주가 다음 해 1주일 수 있음)
        if (month == 12 && endWeek == 1) {
            endWeek = 53; // 12월의 경우 53주까지 고려
        }

        log.info("월별 메모 조회: year={}, month={}, type={}, week범위={}~{}", 
                year, month, type, startWeek, endWeek);
        
        // 메모 타입 변환
        Memo.MemoType memoType = Memo.MemoType.valueOf(type);
        
        // DB에서 해당 주차 범위의 메모 조회
        List<Memo> memos = memoRepository.findMemosInWeekRange(
                memberId, 
                (short) year, 
                (short) startWeek, 
                (short) endWeek, 
                memoType
        );
        
        // DTO 변환
        return memos.stream()
                .map(this::convertToMonthlyItemDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public MemoDetailResponseDto createMemo(MemoInsertRequestDto createDto, Long memberId) {
        // 입력 검증
        validateMemoCreateRequest(createDto);
        
        // 메모 타입 변환
        Memo.MemoType memoType = Memo.MemoType.valueOf(createDto.getType());
        
        // 기존 메모 개수 확인 (중복 방지) - 성능 최적화
        int existingMemoCount = memoRepository.countExistingMemos(
                memberId, 
                (short) createDto.getYear(), 
                (short) createDto.getWeek(), 
                memoType
        );

        if (existingMemoCount > 0) {
            throw new IllegalArgumentException(
                String.format("해당 주차에 이미 %s 메모가 존재합니다. (연도: %d, 주차: %d, 기존 개수: %d)", 
                    createDto.getType(), createDto.getYear(), createDto.getWeek(), existingMemoCount)
            );
        }
        
        // 사용자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        // 메모 엔티티 생성
        Memo memo = Memo.builder()
                .member(member)
                .content(createDto.getContent())
                .year((short) createDto.getYear())
                .week((short) createDto.getWeek())
                .type(memoType)
                .build();
        
        // 저장
        Memo savedMemo = memoRepository.save(memo);
        
        log.info("메모 생성 완료: id={}, memberId={}, type={}", 
                savedMemo.getId(), memberId, savedMemo.getType());
        
        return convertToDetailResponseDto(savedMemo);
    }
    
    @Override
    @Transactional
    public MemoDetailResponseDto updateMemo(Long memoId, MemoUpdateRequestDto updateDto, Long memberId) {
        // 메모 조회 및 권한 확인
        Memo memo = memoRepository.findByIdAndMemberId(memoId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("수정할 메모를 찾을 수 없습니다."));
        
        // 부분 업데이트
        boolean isUpdated = false;
        // System.out.println("업데이트할 내용: " + updateDto.toString() + ", 내용이 비어있는치 체크: " + updateDto.getContent().trim().isEmpty());
        if (updateDto.getContent() != null && !updateDto.getContent().trim().isEmpty()) {
            if (updateDto.getContent().length() > 255) {
                throw new IllegalArgumentException("메모 내용은 255자를 초과할 수 없습니다.");
            }
            memo.setContent(updateDto.getContent().trim());
            isUpdated = true;
        } else if (updateDto.getContent().trim().isEmpty()) {
            memo.setContent("");
            isUpdated = true;
        }
        
        if (updateDto.getType() != null && isValidMemoType(updateDto.getType())) {
            Memo.MemoType memoType = Memo.MemoType.valueOf(updateDto.getType());
            if (!memo.getType().equals(memoType)) {
                memo.setType(memoType);
                isUpdated = true;
            }
        }
        
        if (!isUpdated) {
            throw new IllegalArgumentException("수정할 내용이 없습니다.");
        }
        
        // 저장 (updatedAt은 @PreUpdate에서 자동 설정)
        Memo updatedMemo = memoRepository.save(memo);
        
        log.info("메모 수정 완료: id={}, memberId={}, type={}", 
                updatedMemo.getId(), memberId, updatedMemo.getType());
        
        return convertToDetailResponseDto(updatedMemo);
    }
    
    /**
     * Memo 엔티티를 MemoMonthlyItemDto로 변환
     */
    private MemoMonthlyItemDto convertToMonthlyItemDto(Memo memo) {
        return new MemoMonthlyItemDto(
                memo.getId(),
                memo.getYear().intValue(),
                memo.getWeek().intValue(),
                memo.getType().name(),
                memo.getContent(),
                memo.getCreatedAt()
        );
    }
    
    /**
     * Memo 엔티티를 MemoDetailResponseDto로 변환
     */
    private MemoDetailResponseDto convertToDetailResponseDto(Memo memo) {
        return new MemoDetailResponseDto(
                memo.getId(),
                memo.getContent(),
                memo.getYear().intValue(),
                memo.getWeek().intValue(),
                memo.getType().name(),
                memo.getCreatedAt(),
                memo.getUpdatedAt()
        );
    }
    
    /**
     * 메모 타입 유효성 검증
     */
    private boolean isValidMemoType(String type) {
        try {
            Memo.MemoType.valueOf(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 메모 생성 요청 검증
     */
    private void validateMemoCreateRequest(MemoInsertRequestDto createDto) {
        if (createDto.getContent() == null || createDto.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("메모 내용은 필수입니다.");
        }
        
        if (createDto.getContent().length() > 255) {
            throw new IllegalArgumentException("메모 내용은 255자를 초과할 수 없습니다.");
        }
        
        if (createDto.getYear() < 1000 || createDto.getYear() > 9999) {
            throw new IllegalArgumentException("연도는 4자리 숫자여야 합니다.");
        }
        
        if (createDto.getWeek() < 1 || createDto.getWeek() > 53) {
            throw new IllegalArgumentException("주차는 1~53 범위여야 합니다.");
        }
        
        if (!isValidMemoType(createDto.getType())) {
            throw new IllegalArgumentException("메모 타입은 '다이어리' 또는 '스케줄'이어야 합니다.");
        }
    }
}