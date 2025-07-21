package com.plana.diary.service;

import com.plana.auth.entity.Member;
import com.plana.auth.repository.MemberRepository;
import com.plana.diary.dto.request.*;
import com.plana.diary.entity.*;
import com.plana.diary.enums.TagStatus;
import com.plana.diary.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // 생성자 자동 주입 (final 필드)
public class DiaryService {
    private final DiaryRepository diaryRepository;
    private final DailyRepository dailyRepository;
    private final BookRepository bookRepository;
    private final MovieRepository movieRepository;
    private final DiaryTagRepository diaryTagRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void createDiary(DiaryCreateRequestDto request, Long writerId){
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

        // 4. 태그된 사용자 저장
        for(DiaryTagRequestDto tagDto : request.getDiaryTags()){
            // 태그된 사용자 조회
            Member taggedMember = memberRepository.findById(tagDto.getMemberId())
                    .orElseThrow(() -> new IllegalArgumentException("태그 대상자 정보가 없습니다."));

            // 작성자면 wirter, 아니면 pending 상태로 저장
            TagStatus status = writer.getId().equals(taggedMember.getId())?
                    TagStatus.WRITER : TagStatus.PENDING;

            DiaryTag tag = DiaryTag.builder()
                    .diary(diary)
                    .member(taggedMember)
                    .tagStatus(status)
                    .tagText(tagDto.getTagText())
                    .build();
            diaryTagRepository.save(tag);
        }
    }
}
