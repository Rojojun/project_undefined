package com.finalproject.breeding.user;

import com.finalproject.breeding.dto.SocialLoginRequestDto;
import com.finalproject.breeding.etc.model.Timestamped;
import com.finalproject.breeding.image.model.UserImage;
import com.sun.istack.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
public class User extends Timestamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(unique = true)
    @NotNull
    private String username;

    @Column
    @NotNull
    private String password;

    @Column
    @NotNull
    @Size(min = 3, max = 20, message = "2 ~ 8 사이로 입력해주세요.")
    private String nickname;

    @Column
    private Long kakaoId;

    @OneToOne
    @JoinColumn(name = "USERIMAGE_ID")
    private UserImage userImage;
    @Column
    @NotNull
    private int tier;

    //이메일 인증 여부
    @Column
    private boolean verification;

    @Column(nullable = true)
    private String phoneNumber;

    @Column
    @NotNull
    private Long exp;

    @Column
    private Long follower;

    @Column
    private Long following;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RegisterType registerType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @Builder
    public User(String username, String password, String nickname, RegisterType registerType, UserRole userRole, Boolean verification, String phoneNumber, UserImage userImage) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.userImage = userImage;
        this.userRole = userRole;
        this.registerType = registerType;
        this.exp = 0L;
        this.tier = 0;
        this.follower = 0L;
        this.following = 0L;
        this.verification = verification;
        this.phoneNumber = phoneNumber;
    }

    //Google
    public User(SocialLoginRequestDto socialLoginRequestDto) {
        this.username = socialLoginRequestDto.getEmail();
        this.nickname = "googleUser" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 5);
        this.phoneNumber = "0000";
        this.userRole = UserRole.ROLE_USER;
        this.registerType = RegisterType.SOCIAL;
        this.exp = 0L;
        this.tier = 0;
        this.follower = 0L;
        this.following = 0L;
        this.verification = true;
    }

    //kakao
    public User(UserRole role, RegisterType registerType, Long kakaoId, String socialRandomValue) {
        this.username = socialRandomValue;
        this.nickname = socialRandomValue;
        this.userRole = role;
        this.registerType = registerType;
        this.phoneNumber = "0000";
        this.exp = 0L;
        this.tier = 0;
        this.follower = 0L;
        this.following = 0L;
        this.kakaoId = kakaoId;
        this.verification = true;
    }

    public void editUserNickname(String nickname) {
        this.nickname = nickname;
    }

    public void editUserPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    //--------exp--------
    public void oneLvUp(User user) {
        this.tier = user.getTier() + 1;
        this.exp = user.getExp() - 10000L;
    }

    public void twoLvUp(User user) {
        this.tier = user.getTier() + 1;
        this.exp = user.getExp() - 20000L;
    }

    public void threeLvUp(User user) {
        this.tier = user.getTier() + 1;
        this.exp = user.getExp() - 40000L;
    }

    public void tenExpUp(User user) {
        this.exp = user.getExp() + 10L;
    }

    public void fiveExpUp(User user) {
        this.exp = user.getExp() + 5L;
    }

    public void following(User user) {
        this.follower = user.getFollower() + 1L;
    }

    public void follower(User user) {
        this.following = user.getFollowing() + 1L;
    }

    public void unFollowing(User user) {
        this.follower = user.getFollower() - 1L;
    }

    public void unFollower(User user) {
        this.following = user.getFollowing() - 1L;
    }

    public void updateProfileImage(UserImage userImage) {
        this.userImage = userImage;
    }
}
