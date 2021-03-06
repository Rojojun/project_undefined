package com.finalproject.breeding.board.repository;

import com.finalproject.breeding.board.dto.PostResponseDto;
import com.finalproject.breeding.board.model.category.PostNReelsCategory;
import com.finalproject.breeding.board.model.Post;
import com.finalproject.breeding.etc.dto.response.MyPagePostDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository <Post, Long> {
    //List<Post> findByOrderByLikeCntDesc();
    Slice<PostResponseDto> findPostByOrderByIdDesc(PageRequest pageRequest);

    Slice<PostResponseDto> findPostByPostNReelsCategory(PageRequest pageRequest, PostNReelsCategory postNReelsCategory);

    Slice<PostResponseDto> findPostByPostNReelsCategoryOrderByBoardMainLikeCntDesc(PageRequest pageRequest, PostNReelsCategory postNReelsCategory);

    Slice<PostResponseDto> findPostByOrderByBoardMainLikeCntDesc(PageRequest pageRequest);

    @Query("select p " + "from Post p " + "where p.boardMain.createdAt > :date and p.postNReelsCategory = :postNReelsCategory " +"order by p.boardMain.likeCnt desc " )
    List<PostResponseDto> findPostByPostNReelsCategoryOrderByBoardMainLikeCntDesc(LocalDateTime date, PostNReelsCategory postNReelsCategory, PageRequest pageRequest);

    @Query("select p " + "from Post p " + "where p.boardMain.createdAt > :date " + "order by p.boardMain.likeCnt desc " )
    List<PostResponseDto> findPostByOrderByBoardMainLikeCntDesc(LocalDateTime date, PageRequest pageRequest);


    Post findByBoardMainId(Long boardMainId);


    Slice<MyPagePostDto> findByUserNicknameOrderByBoardMainCreatedAtDesc(PageRequest pageRequest, String nickname);
}
