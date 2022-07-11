package com.finalproject.breeding.user.dto.responseDto;

import com.finalproject.breeding.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserInfo {
    private String username;
    private String nickname;
    private int tier;
    private Long exp;
    private String img;

    public UserInfo(User user){
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.tier = user.getTier();
        this.exp = user.getExp();
        this.img = user.getUserImage().getUrl();
    }
}