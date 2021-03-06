package com.finalproject.breeding.etc.service;

import com.finalproject.breeding.board.model.BoardMain;
import com.finalproject.breeding.board.repository.BoardMainRepository;
import com.finalproject.breeding.dto.CommentResponseDto;
import com.finalproject.breeding.etc.dto.CommentRequestDto;
import com.finalproject.breeding.etc.dto.response.MyDto;
import com.finalproject.breeding.error.ErrorCode;
import com.finalproject.breeding.etc.model.Comment;
import com.finalproject.breeding.etc.model.Reply;
import com.finalproject.breeding.etc.repository.CommentRepository;
import com.finalproject.breeding.etc.dto.response.CommentMapping;
import com.finalproject.breeding.etc.repository.ReplyRepository;
import com.finalproject.breeding.user.User;
import com.finalproject.breeding.user.repository.UserRepository;
import com.finalproject.breeding.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final BoardMainRepository boardMainRepository;
    private final UserRepository userRepository;

    private final ReplyRepository replyRepository;

    //λκΈ μμ±
    @Transactional
    public ResponseEntity<MyDto> createComment(CommentRequestDto requestDto, Long boardMainId, String username) {

        MyDto dto = new MyDto();
        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        Optional<BoardMain> boardMain1 = boardMainRepository.findById(boardMainId);
        Optional<User> user1 = userRepository.findByUsername(username);
        dto.setStatus(ErrorCode.OK);
        if(!boardMain1.isPresent()){
            dto.setData(null);
            dto.setMessage("κ²μκΈμ΄ μμ΅λλ€!");
        }else if(!user1.isPresent()) {
            dto.setData(null);
            dto.setMessage("μ μ κ° μμ΅λλ€!");
        }else{
            BoardMain boardMain = boardMain1.get();
            User user = user1.get();
            Comment comment = new Comment(requestDto,boardMain ,user);

            Long commentId = commentRepository.save(comment).getId();
            boardMain.plusCommentCnt(boardMain);


            dto.setData(commentId);
            dto.setMessage("λκΈ λ±λ‘!");
        }
        return new ResponseEntity<>(dto, header, HttpStatus.OK);
    }


    //λκΈ μ­μ 
    @Transactional
    public ResponseEntity<MyDto> deleteComment(Long commentId) {

        MyDto dto = new MyDto();
        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        String userId = SecurityUtil.getCurrentUsername(); //νμ  λ‘κ·ΈμΈν μ μ  pk
        Optional<Comment> comment =commentRepository.findById(commentId);
        dto.setStatus(ErrorCode.OK);
        dto.setData(commentId);

        if(!comment.isPresent()){
            dto.setMessage("λκΈμ΄ μμ΅λλ€!");
        }else if (!Objects.equals(userId, comment.get().getUser().getUsername())) {
            dto.setMessage("μ¬μ©μμ λκΈμ΄ μλλλ€!");
        }else{
            BoardMain boardMain = comment.get().getBoardMain();

            Long replyCnt =(long)replyRepository.findAllByCommentId(commentId).size();
            boardMain.minusCommentCnt(boardMain,replyCnt);
            List<Reply> replyies = replyRepository.findByCommentId(commentId); //λκΈμμλ λλκΈ μ­μ 
            replyRepository.deleteAll(replyies);

            commentRepository.deleteById(commentId);
            dto.setMessage("λκΈ μ­μ !");
        }
        return new ResponseEntity<>(dto, header, HttpStatus.OK);

    }

    //λκΈ μμ 
    @Transactional
    public ResponseEntity<MyDto> patchComment(CommentRequestDto requestDto, Long commentId) {

        MyDto dto = new MyDto();
        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

        String userId = SecurityUtil.getCurrentUsername(); //νμ  λ‘κ·ΈμΈν μ μ  pk
        Optional<Comment> comment =commentRepository.findById(commentId);
        dto.setStatus(ErrorCode.OK);
        dto.setData(commentId);

        if(!comment.isPresent()){
            dto.setMessage("λκΈμ΄ μμ΅λλ€!");
        }else if (!Objects.equals(userId, comment.get().getUser().getUsername())) {   //λκΈμ λλ€μμ μΌμΉνλ€λ©΄
            dto.setMessage("μ¬μ©μμ λκΈμ΄ μλλλ€!");
        }else{
            Comment comment1 = comment.get();
            comment1.updateComment(requestDto);
            dto.setMessage("λκΈ μμ !");
        }
        return new ResponseEntity<>(dto, header, HttpStatus.OK);

    }


    //λκΈ λΆλ¬μ€κΈ°
    @Transactional
    public CommentResponseDto getAllCommnet(Long boardMainId, Long page) {
        PageRequest pageRequest = PageRequest.of(Math.toIntExact(page), 10, Sort.by(Sort.Direction.DESC, "Id"));

        Slice<CommentMapping> comments = commentRepository.findByBoardMainId(pageRequest, boardMainId);
        CommentResponseDto commentResponseDto = new CommentResponseDto(comments.getContent(), comments.isLast());
        return commentResponseDto;
    }


}
