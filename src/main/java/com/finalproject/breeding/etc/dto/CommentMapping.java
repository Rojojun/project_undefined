package com.finalproject.breeding.etc.dto;

import java.time.LocalDateTime;
import com.finalproject.breeding.etc.image.model.UserImage;

public interface CommentMapping {

    Long getId();
    Long getUserId();
    String getComment();
    String getUserNickname();
    UserImage getUserUserImage();
    LocalDateTime getCreatedAt();










}
