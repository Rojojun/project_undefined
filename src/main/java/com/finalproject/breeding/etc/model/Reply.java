package com.finalproject.breeding.etc.model;

import com.finalproject.breeding.board.model.BoardMain;
import com.finalproject.breeding.etc.dto.CommentRequestDto;
import com.finalproject.breeding.etc.dto.ReplyRequestDto;
import com.finalproject.breeding.etc.repository.ReplyRepository;
import com.finalproject.breeding.user.User;
import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Reply extends Timestamped {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @JoinColumn(name = "USER_ID")
    @ManyToOne
    private User user;

    @JoinColumn(name = "COMMENT_ID")
    @ManyToOne
    private Comment comment;

    @Column
    @NotNull
    private String reply;

    public Reply(ReplyRequestDto requestDto, Comment comment, User user){

        this.reply = requestDto.getReply();
        this.comment = comment;
        this.user = user;
    }

    public void updateReply(ReplyRequestDto requestDto){
        this.reply = requestDto.getReply();
    }


}
