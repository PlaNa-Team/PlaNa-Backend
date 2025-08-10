package com.plana.diary.service;

import com.plana.auth.entity.Member;
import com.plana.auth.repository.MemberRepository;
import com.plana.diary.dto.request.*;
import com.plana.diary.dto.response.*;
import com.plana.diary.entity.*;
import com.plana.diary.enums.DiaryType;
import com.plana.diary.enums.TagStatus;
import com.plana.diary.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // 생성자 자동 주입 (final 필드)
public class DiaryServiceImpl implements DiaryService {
    private final DiaryRepository diaryRepository;
    private final DailyRepository dailyRepository;
    private final BookRepository bookRepository;
    private final MovieRepository movieRepository;
    private final DiaryTagRepository diaryTagRepository;
    private final MemberRepository memberRepository;


    // 다이어리 등록
    @Transactional
    public DiaryCreateResponseDto createDiary(DiaryCreateRequestDto request, Long writerId){
        // 1. 작성자 조회
        Member writer = memberRepository.findById(writerId)
                .orElseThrow(() -> new IllegalArgumentException("작성자 정보가 없습니다."));

        // 2. Diary 엔티티 저장 (공통 필드만 저장)
        Diary diary = Diary.builder()
                .writer(writer)
                .diaryDate(request.getDiaryDate())
                .type(request.getDiaryType())
                .imageUrl(request.getImageUrl())
                .build();
        diaryRepository.save(diary);

        // 3. 타입에 대한 서브 테이블 저장 ( Daily/ Book/ Movie)
        switch (request.getDiaryType()){
            case DAILY -> {
                // 다운캐스팅 후 Daily 저장
                DailyContentRequestDto dailyDto = (DailyContentRequestDto) request.getContent();
                Daily daily = Daily.builder()
                        .diary(diary)
                        .title(dailyDto.getTitle())
                        .location(dailyDto.getLocation())
                        .memo((dailyDto.getMemo()))
                        .build();
                dailyRepository.save(daily);
            }

            case BOOK -> {
                BookContentRequestDto bookDto = (BookContentRequestDto) request.getContent();
                Book book = Book.builder()
                        .diary(diary)
                        .title(bookDto.getTitle())
                        .author(bookDto.getAuthor())
                        .genre(bookDto.getGenre())
                        .publisher(bookDto.getPublisher())
                        .startDate(bookDto.getStartDate())
                        .endDate(bookDto.getEndDate())
                        .rating(bookDto.getRating())
                        .comment(bookDto.getComment())
                        .build();
                bookRepository.save(book);
            }

            case MOVIE -> {
                MovieContentRequestDto movieDto = (MovieContentRequestDto) request.getContent();
                Movie movie = Movie.builder()
                        .diary(diary)
                        .title(movieDto.getTitle())
                        .director(movieDto.getDirector())
                        .actors(movieDto.getActors())
                        .genre(movieDto.getGenre())
                        .rewatched(movieDto.isRewatch())
                        .rating(movieDto.getRating())
                        .comment(movieDto.getComment())
                        .build();
                movieRepository.save(movie);
            }
        }

        // 4. 태그 저장
        List<CreateDiaryTagResponseDto> tagDtos = new ArrayList<>();

        for (DiaryTagRequestDto tagDto : Optional.ofNullable(request.getDiaryTags()).orElse(Collections.emptyList())) {

            DiaryTag tag;

            if (tagDto.getMemberId() != null) {
                // 4-1. 회원 태그
                Member taggedMember = memberRepository.findById(tagDto.getMemberId())
                        .orElseThrow(() -> new IllegalArgumentException("태그 대상자 정보가 없습니다."));

                TagStatus status = writer.getId().equals(taggedMember.getId())
                        ? TagStatus.WRITER
                        : TagStatus.PENDING;

                tag = DiaryTag.builder()
                        .diary(diary)
                        .member(taggedMember)  // 연관관계 필드에는 엔티티 객체
                        .tagStatus(status)
                        .build();

                diaryTagRepository.save(tag);

                // response DTO (회원이면 memberId 반환)
                tagDtos.add(new CreateDiaryTagResponseDto(taggedMember.getId()));

            } else if (tagDto.getTagText() != null && !tagDto.getTagText().isBlank()) {
                // 4-2. 사용자 입력 태그 (회원 없는 태그)
                tag = DiaryTag.builder()
                        .diary(diary)
                        .tagText(tagDto.getTagText())   // 입력 태그 저장
                        .tagStatus(TagStatus.PENDING) // 초기 상태
                        .build();

                diaryTagRepository.save(tag);

                // response DTO (비회원 태그는 memberId 없음 → null)
                tagDtos.add(new CreateDiaryTagResponseDto(null));
            }
        }


        // 5. 생성된 다이어리 응답 DTO 반환
        return new DiaryCreateResponseDto(
                diary.getId(),
                diary.getDiaryDate(),
                diary.getType().name(),
                diary.getCreatedAt(),
                diary.getImageUrl(),
                request.getContent(),  // 그대로 넣거나, 타입 맞춰 변환
                tagDtos
        );
    }

    // 다이어리 상세 조회
    @Override
    public DiaryDetailResponseDto getDiaryDetail(Long diaryId, Long memberId){
        // 1. 다이어리 존재 여부 확인
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("다이어리를 찾을 수 없습니다."));

        // 2. 태그 정보 확인 (작성자 or 태그된 사용자만 조회 가능)
        boolean isWriter = diary.getWriter().getId().equals(memberId);

        // 3. 작성자가 아니면 태그 조회
        List<DiaryTag> myTags = diaryTagRepository.findByDiary_IdAndMember_Id(diaryId, memberId);

        // 4. 권한 체크
        if (!isWriter && myTags.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "조회 권한이 없습니다.");
        }

        // 태그가 있고 + 삭제 상태 + 작성자 아님 → 권한 없음
        boolean allDeleted = myTags.stream()
                .allMatch(tag -> tag.getTagStatus() == TagStatus.DELETED);

        if (!isWriter && allDeleted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제된 다이어리는 조회할 수 없습니다.");
        }

        // 5. 타입별(일상, 책, 영화) dto 변환
        DiaryContentResponseDto contentDto = mapContentToDto(diary);

        // 6. 태그 리스트 매핑
        // stream은 리스트를 순차 처리하는 파이프라인 / map은 각 요소를 변환 / toList는 최종적으로 리스트로 모음
        List<DiaryTagResponseDto> tagDtos = diaryTagRepository.findByDiary_Id(diaryId).stream()
                .map(tag -> {
                    if(tag.getMember() != null){
                        Member m = tag.getMember();
                        return DiaryTagResponseDto.builder()
                                .id(tag.getId())
                                .memberId(m.getId())
                                .loginId(m.getLoginId())
                                .memberNickname(m.getNickname())
                                .tagStatus(tag.getTagStatus())
                                .build();
                    }else {
                        return DiaryTagResponseDto.builder()
                                .id(tag.getId())
                                .tagText(tag.getTagText())
                                .tagStatus(tag.getTagStatus())
                                .build();
                    }
                }).toList();


        return new DiaryDetailResponseDto(
                diary.getId(),
                diary.getDiaryDate(),
                diary.getType(),
                diary.getImageUrl(),
                diary.getCreatedAt(),
                diary.getUpdatedAt(),
                contentDto,
                tagDtos
        );
    }


    private DiaryContentResponseDto mapContentToDto(Diary diary) {
        return switch (diary.getType()) {
            case DAILY -> {
                Daily daily = dailyRepository.findByDiary_Id(diary.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Daily 내용을 찾을 수 없습니다."));
                yield DailyContentResponseDto.builder()
                        .title(daily.getTitle())
                        .location(daily.getLocation())
                        .memo(daily.getMemo())
                        .build();
            }
            case BOOK -> {
                Book book = bookRepository.findByDiary_Id(diary.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Book 내용을 찾을 수 없습니다."));
                yield BookContentResponseDto.builder()
                        .title(book.getTitle())
                        .author(book.getAuthor())
                        .publisher(book.getPublisher())
                        .genre(book.getGenre())
                        .startDate(book.getStartDate())
                        .endDate(book.getEndDate())
                        .rating(book.getRating())
                        .comment(book.getComment())
                        .build();
            }
            case MOVIE -> {
                Movie movie = movieRepository.findByDiary_Id(diary.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Movie 내용을 찾을 수 없습니다."));
                yield MovieContentResponseDto.builder()
                        .title(movie.getTitle())
                        .director(movie.getDirector())
                        .actors(movie.getActors())
                        .genre(movie.getGenre())
                        .rewatch(movie.isRewatched())
                        .rating(movie.getRating())
                        .comment(movie.getComment())
                        .build();
            }
        };
    }

    //월간 다이어리 조회
    @Override
    public DiaryMonthlyResponseDto getMonthlyDiaries(Long memberId, int year, int month){
        if ( month < 1 || month > 12 ){
            throw new IllegalArgumentException("월은 1~12 사이여야 합니다.");
        }

        // 사용자 존재 확인
        memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        // 해당 월의 시작과 끝
        //LocalDate.of(년, 월, 일) : 해당 날짜의 LocalDate 객체 생성
        LocalDate start = LocalDate.of(year, month, 1);
        //start.with : start 날짜를 기준으로 일부 값을 변경한 새로운 LocalDate 반환, TemporalAdjusters.lastDayOfMonth() : 그 달의 마지막 날로 변경
        LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());

        //다이어리 조회 : SQL 실행 결과 -> Diary 엔티티로 매핑 -> List 형태로 변환 -> diaries 변수에 저장
        List<Diary> diaries = diaryRepository.findMonthlyDiaries(memberId, start, end);

        if(diaries.isEmpty()){
            return DiaryMonthlyResponseDto.builder()
                    .diaryList(Collections.emptyList())
                    .build();
        }

        //타입별 diaryId 모으기
        List<Long> dailyIds = new ArrayList<>();
        List<Long> bookIds = new ArrayList<>();
        List<Long> movieIds = new ArrayList<>();
        for (Diary d : diaries) {
            switch (d.getType()) {
                case DAILY -> dailyIds.add(d.getId());
                case BOOK  -> bookIds.add(d.getId());
                case MOVIE -> movieIds.add(d.getId());
            }
        }

        //배치 조회하여 title 맵 구성 (diaryId -> title)
        Map<Long, String> titleByDiaryId = new HashMap<>();

        if(!dailyIds.isEmpty()){
            dailyRepository.findByDiary_IdIn(dailyIds).forEach(d ->
                    titleByDiaryId.put(d.getDiary().getId(), d.getTitle()));
        }
        if(!bookIds.isEmpty()){
            bookRepository.findByDiary_IdIn(bookIds).forEach(d ->
                    titleByDiaryId.put(d.getDiary().getId(), d.getTitle()));
        }

        if(!movieIds.isEmpty()){
            movieRepository.findByDiary_IdIn(movieIds).forEach(d ->
                    titleByDiaryId.put(d.getDiary().getId(), d.getTitle()));
        }

        //Dto 매핑
        List<DiaryMonthlyItemDto> items = diaries.stream()
                .map( d -> DiaryMonthlyItemDto.builder()
                        .id(d.getId())
                        .diaryDate(d.getDiaryDate())
                        .type(d.getType().name())
                        .imageUrl(d.getImageUrl())
                        .title(titleByDiaryId.getOrDefault(d.getId(), "")) // getOrDefault는 키에 해당하는 값이 있으면 그 값을 반환하고, 없으면 기본값을 반환
                        .build())
                .collect(Collectors.toList());

        return DiaryMonthlyResponseDto.builder()
                .diaryList(items)
                .build();
    }

    // 다이어리 삭제
    @Override
    public void deleteDiary(Long diaryId, Long memberId){
        // 다이어리 대상 조회
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "다이어리를 찾을 수 없습니다."));

        // 권한체크
        if (!diary.getWriter().getId().equals(memberId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        // 연관 데이터 삭제
        // 1) 태그
        List<DiaryTag> tags = diaryTagRepository.findByDiary_Id(diaryId);
        if (!tags.isEmpty()) diaryTagRepository.deleteAll(tags);

        // 2)typq별
        // ifPresent는 값이 있으면 코드를 실행하라는 의미
        switch (diary.getType()) {
            case DAILY -> dailyRepository.findByDiary_Id(diaryId).ifPresent(dailyRepository::delete);
            case BOOK -> bookRepository.findByDiary_Id(diaryId).ifPresent(bookRepository::delete);
            case MOVIE -> movieRepository.findByDiary_Id(diaryId).ifPresent(movieRepository::delete);
        }

        // 본문 삭제
        diaryRepository.delete(diary);
    }

    // 다이어리 수정
    @Override
    public DiaryDetailResponseDto updateDiary(Long diaryId, Long memberId, DiaryUpdateRequestDto requestDto){
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "다이어리를 찾을 수 없습니다."));

        if (!diary.getWriter().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }

        // 타입 변경 금지
        if(requestDto.getDiaryType() != null && requestDto.getDiaryType() != diary.getType()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "타입은 수정할 수 없습니다.");
        }

        // 공통 필드 부분 수정
        if (requestDto.getDiaryDate() != null) diary.setDiaryDate(requestDto.getDiaryDate());
        if (requestDto.getImageUrl() != null) diary.setImageUrl(requestDto.getImageUrl());

        // content만 현재 타입 기준으로 부분 수정
        if(requestDto.getContent() != null){
            switch (diary.getType()){
                case DAILY -> {

                }
            }
        }

    }

}
