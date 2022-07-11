package com.finalproject.breeding.etc.service;

import com.finalproject.breeding.board.model.BoardMain;
import com.finalproject.breeding.board.repository.BoardMainRepository;
import com.finalproject.breeding.etc.dto.CommentRequestDto;
import com.finalproject.breeding.etc.dto.MyDto;
import com.finalproject.breeding.error.ErrorCode;
import com.finalproject.breeding.etc.model.Comment;
import com.finalproject.breeding.etc.repository.CommentRepository;
import com.finalproject.breeding.user.User;
import com.finalproject.breeding.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.Charset;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final BoardMainRepository boardMainRepository;
    private final UserRepository userRepository;

    //댓글 작성
    @Transactional
    public ResponseEntity<MyDto> createComment( CommentRequestDto requestDto, Long boardMainId, String username) {

        MyDto dto = new MyDto();
        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        User user = userRepository.findByUsername(username).orElseThrow(
                ()-> new NullPointerException("유저가 존재하지 않습니다."));

        BoardMain boardMain = boardMainRepository.findById(boardMainId).orElseThrow(
                ()-> new NullPointerException("게시글이 존재하지 않습니다."));

        Comment comment = new Comment(requestDto,boardMain,user);
        commentRepository.save(comment);

        dto.setStatus(ErrorCode.OK);
        dto.setData(boardMainId);
        dto.setMessage("댓글 등록!");
        return new ResponseEntity<>(dto, header, HttpStatus.OK);


    }

    //댓글 수정
    @Transactional
    public void patchComment(CommentRequestDto requestDto, Long id) {
        Comment comment = commentRepository.findById(id).orElseThrow(
                ()-> new NullPointerException("댓글이 존재하지 않습니다."));


        comment.updateComment(requestDto);

    }


}