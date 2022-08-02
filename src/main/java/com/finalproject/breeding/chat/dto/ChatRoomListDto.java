package com.finalproject.breeding.chat.dto;

import com.finalproject.breeding.chat.model.ChatRoom;
import com.finalproject.breeding.user.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ChatRoomListDto {
    private Long id;
    private Long boardMainid;
    private List<User> memberList;
    private String nickname;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public ChatRoomListDto(Long boardMain , ChatRoom chatRoom, User member) {
        this.id = chatRoom.getId();
        this.boardMainid = boardMain;
        this.memberList = chatRoom.getUserList();
        this.nickname = member.getNickname();
        this.createdAt = chatRoom.getCreatedAt();
        this.modifiedAt = chatRoom.getModifiedAt();
    }
}
