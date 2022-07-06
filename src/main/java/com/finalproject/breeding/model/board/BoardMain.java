package com.finalproject.breeding.model.board;

import com.finalproject.breeding.dto.CommunityRequestDto;
import com.finalproject.breeding.dto.WithPostRequestDto;
import com.finalproject.breeding.model.Timestamped;
import com.finalproject.breeding.model.User;
import com.finalproject.breeding.model.category.BoardKind;
import com.sun.istack.NotNull;
import lombok.*;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class BoardMain extends Timestamped {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @JoinColumn(name = "BOARDKIND_ID")
    @ManyToOne
    private BoardKind boardKind;

    @JoinColumn(name = "USER_ID")
    @ManyToOne
    private User user;

    @Column
    @NotNull
    private Long likeCnt;

    @Column
    @NotNull
    private String content;

//    @Column
//    @NotNull
//    private Long viewCnt;

//    @OneToMany
//    @JoinColumn(name = "IMG_URL")
//    private List<ImgUrl> imgUrls;

    public BoardMain(BoardKind boardKind, CommunityRequestDto communityRequestDto, User user){
        this.boardKind = boardKind;
        this.content = communityRequestDto.getContent();
        this.likeCnt = 0L;
        this.user = user;
    }

    public void update(CommunityRequestDto communityRequestDto) {
        this.content = communityRequestDto.getContent();
    }
    public BoardMain(BoardKind boardKind, WithPostRequestDto withPostRequestDto, User user){
        this.boardKind = boardKind;
        this.content = withPostRequestDto.getContent();
        this.likeCnt = 0L;
        this.user = user;
    }
    public void plusLikeCnt(BoardMain boardMain){
        this.likeCnt = boardMain.getLikeCnt()+1L;
    }
    public void minusLikeCnt(BoardMain boardMain){
        this.likeCnt = boardMain.getLikeCnt()-1L;
    }

}
