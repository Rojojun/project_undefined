package com.finalproject.breeding.video.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.StringReader;

@Getter
@NoArgsConstructor
public class VideoResponseDto {
    private String video;
    private String thumbnail;
    public VideoResponseDto(String video, String thumbnail) {
        this.video = video;
        this.thumbnail = thumbnail;
    }
}
