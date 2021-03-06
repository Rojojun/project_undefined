package com.finalproject.breeding.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finalproject.breeding.dto.*;
import com.finalproject.breeding.image.model.UserImage;
import com.finalproject.breeding.image.repository.UserImageRepository;
import com.finalproject.breeding.service.VerificationEmailSenderService;
import com.finalproject.breeding.user.*;
import com.finalproject.breeding.user.dto.requestDto.LoginDto;
import com.finalproject.breeding.user.dto.requestDto.ProfileImageDto;
import com.finalproject.breeding.user.dto.requestDto.SignupRequestDto;
import com.finalproject.breeding.user.dto.requestDto.TokenRequestDto;
import com.finalproject.breeding.user.dto.responseDto.TokenDto;
import com.finalproject.breeding.error.CustomException;
import com.finalproject.breeding.error.ErrorCode;
import com.finalproject.breeding.etc.model.RefreshToken;
import com.finalproject.breeding.user.dto.responseDto.UserInfo;
import com.finalproject.breeding.user.repository.RefreshTokenRepository;
import com.finalproject.breeding.user.repository.UserRepository;
import com.finalproject.breeding.user.security.UserDetailsImpl;
import com.finalproject.breeding.user.token.TokenProvider;
import com.finalproject.breeding.socialUtil.GoogleRestTemplate;
import lombok.RequiredArgsConstructor;
import net.nurigo.java_sdk.api.Message;
import net.nurigo.java_sdk.exceptions.CoolsmsException;
import org.apache.http.protocol.HTTP;
import org.apache.tomcat.jni.Local;
import org.json.simple.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserImageRepository userImageRepository;

    //????????? ?????? ?????? ????????? ?????????
    private final VerificationEmailSenderService verificationEmailSenderService;

    //???????????? ????????? ?????? ????????? ????????? ????????????
    public static HashMap<String,String> phoneVerificationDB = new HashMap<>();

    //???????????????
    public static HashMap<String, LocalDateTime> verificationDuration = new HashMap<>();

    //???????????? ????????? ?????? ???????????? ????????? authToken
    public static HashMap<String,String> emailVerificationDB = new HashMap<>();

    //Google Social Login
    private final GoogleRestTemplate googleRestTemplate;


    //----------------------------?????? ?????? ?????? ??????-------------------------------
    //????????? ????????????
    @Transactional(readOnly = true)
    public boolean checkUsernameDuplication(String username){
        return userRepository.existsByUsername(username);
    }

    //????????? ????????????
    @Transactional(readOnly = true)
    public boolean checkNicknameDuplication(String nickname){
        return userRepository.existsByNickname(nickname);
    }

    //????????? ????????????
    @Transactional(readOnly = true)
    public boolean checkPhoneNumberDuplication(String phoneNumber){
        return userRepository.existsByPhoneNumber(phoneNumber);
    }


    //----------------------------?????? ?????? ??????-------------------------------
    //????????? ?????????????????? ??????
    public void certifyPhoneNumber(String phoneNumber){
        String api_key = "NCS1HI0WXQRNU4EA";
        String api_secret = "0SNTCCSSJJSUPIAOTBTTF0ILPH8QNOYG";
        Message coolsms = new Message(api_key, api_secret);

        Random rand = new Random();
        String numStr = "";
        for(int i = 0; i < 4; i++){
            String ran = Integer.toString(rand.nextInt(10));
            numStr += ran;
        }

        System.out.println("????????? ??????: " + phoneNumber);
        System.out.println("?????? ??????: " + numStr);
        phoneVerificationDB.put(phoneNumber, numStr);
        verificationDuration.put(phoneNumber, LocalDateTime.now());

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("to", phoneNumber); //??????????????????
        params.put("from", "01083231544"); //??????????????????
        params.put("type", "SMS");
        params.put("text", "ANYZOO ???????????????" + "[" + numStr + "]" + "?????????.");
        params.put("app_version", "test app 1.2");

        try{
            JSONObject obj = coolsms.send(params);
            System.out.println(obj.toString());
        }catch (CoolsmsException e){
            throw new RuntimeException(e.getMessage());
        }
    }

    //????????? ?????????????????? ?????? ?????????
    public boolean compareConfirmNumber(PhoneVerificationDto phoneVerificationDto){
        String tests = phoneVerificationDB.get(phoneVerificationDto.getPhoneNumber());
        if(Optional.ofNullable(tests).isPresent()){
            LocalDateTime timeNow = LocalDateTime.now();
            LocalDateTime beginningTime = verificationDuration.get(phoneVerificationDto.getPhoneNumber());
            Duration duration = Duration.between(timeNow, beginningTime);
            if(tests.equals(phoneVerificationDto.getNumStr())){
                if(duration.getSeconds() > 181){
                    phoneVerificationDB.remove(phoneVerificationDto.getPhoneNumber());
                    verificationDuration.remove(phoneVerificationDto.getPhoneNumber());
                    return false;
                }else{
                    phoneVerificationDB.remove(phoneVerificationDto.getPhoneNumber());
                    verificationDuration.remove(phoneVerificationDto.getPhoneNumber());
                    return true;
                }
            }else{
                return false;
            }
        }
        return false;
    }

    //????????? ?????? ?????? ??????
    @Transactional
    public void sendEmailVerificationLink(String email){
        //????????? ?????? UUID ??????
        String userUUID = UUID.randomUUID().toString();
        //????????? ???????????? UUID ??????
        emailVerificationDB.put(email, userUUID);
        //????????? ?????? ????????? ??????
        verificationEmailSenderService.send(email, userUUID);
    }

    //????????? ?????? ?????? ?????????
    @Transactional
    public boolean confirmEmail(EmailVerificationRequestDto emailVerificationRequestDto){
        String authToken = emailVerificationDB.get(emailVerificationRequestDto.getEmail());
        if(Optional.ofNullable(authToken).isPresent()){
            if(authToken.equals(emailVerificationRequestDto.getAuthToken())){
                emailVerificationDB.remove(emailVerificationRequestDto.getEmail());
                return true;
            }
        }
        return false;
    }


    //----------------------------?????? ?????????/?????? ??????-------------------------------
    @Transactional
    public Map<String, Object> signup(SignupRequestDto signupRequestDto) {

        // ?????? ????????? ?????? ??????
        String username = signupRequestDto.getUsername();
        if (userRepository.existsByUsername(username)) {
            throw new CustomException(ErrorCode.SIGNUP_MEMBERID_DUPLICATE_CHECK);
        }

        // ????????? ?????? ??????
        String nickname = signupRequestDto.getNickname();
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.SIGNUP_NICKNAME_DUPLICATE_CHECK);
        }

        // ????????? ?????? ??????
        String phoneNumber = signupRequestDto.getPhoneNumber();
        if(userRepository.existsByPhoneNumber(phoneNumber)){
            throw new CustomException(ErrorCode.SIGNUP_PHONENUMBER_DUPLICATE_CHECK);
        }

        // ?????? ????????? ??????

        UserImage userImage;
        if (signupRequestDto.getUserImage()==null){
            userImageRepository.save(userImage = new UserImage());
        } else {
            userImage = userImageRepository.findById(signupRequestDto.getUserImage()).orElseThrow(()->new CustomException(ErrorCode.Image_NOT_FOUND));
        }
        userImage.updateToUser(userRepository.save(
                User.builder()
                        .username(signupRequestDto.getUsername())
                        .password(passwordEncoder.encode(signupRequestDto.getPassword()))
                        .nickname(signupRequestDto.getNickname())
                        .userImage(userImage)
                        .verification(true)
                        .phoneNumber(signupRequestDto.getPhoneNumber())
                        .userRole(UserRole.ROLE_USER)
                        .registerType(RegisterType.GENERAL)
                        .build()
        ));

        //JWT ?????? ??????
        TokenDto tokenDto = tokenProvider.generateTokenDto(authenticationManagerBuilder.getObject().authenticate(signupRequestDto.toAuthentication()));

        // 4. RefreshToken ??????
        refreshTokenRepository.save(RefreshToken.builder()
                .key(signupRequestDto.getUsername())
                .value(tokenDto.getRefreshToken())
                .build()
        );

        // 5. ?????? ??????
        Map<String, Object> data = new HashMap<>();
        data.put("token", tokenDto);
        return data;
    }

    @Transactional
    public Map<String, Object> login(LoginDto loginDto) {
        UserValidator.validateUsernameEmpty(loginDto);
        UserValidator.validatePasswordEmpty(loginDto);

        User user = userRepository.findByUsername(loginDto.getUsername()).orElseThrow(
                () -> new CustomException(ErrorCode.LOGIN_NOT_FOUNT_MEMBERID)
        );

        //????????? ?????? ?????? ?????? ????????????
        if(!user.isVerification()){
            throw new CustomException(ErrorCode.NOT_VERIFIED_USER_INFORMATION);
        }

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_PASSWORD_NOT_MATCH);
        }
        // 1. Login ID/PW ??? ???????????? AuthenticationToken ??????
        UsernamePasswordAuthenticationToken authenticationToken = loginDto.toAuthentication();

        // 2. ????????? ?????? (????????? ???????????? ??????) ??? ??????????????? ??????
        //    authenticate ???????????? ????????? ??? ??? UserDetailsServiceImpl ?????? ???????????? loadUserByUsername ???????????? ?????????
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. ?????? ????????? ???????????? JWT ?????? ??????
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 4. RefreshToken ??????
        refreshTokenRepository.save(RefreshToken.builder()
                .key(authentication.getName())
                .value(tokenDto.getRefreshToken())
                .build()
        );

        // 5. ?????? ??????
        Map<String, Object> data = new HashMap<>();
        data.put("token", tokenDto);

        return data;
    }

    //Google ?????????
    @Transactional
    public SocialTokenDto googleLogin(String code) {
        SocialLoginRequestDto socialLoginRequestDto = googleRestTemplate.googleUserInfoByAccessToken(googleRestTemplate.findAccessTokenByCode(code).getAccess_token());
        User user = userRepository.findByUsername(socialLoginRequestDto.getEmail())
                .orElseGet(() -> {
                            User tempUser = userRepository.save(new User(socialLoginRequestDto));
                            UserImage userImage = userImageRepository.save(new UserImage(tempUser));
                            tempUser.updateProfileImage(userImage);
                            return tempUser;
                        }
                );
        return createToken(new UserRequestDto(user));
    }

    @Transactional
    public SocialTokenDto kakaoLogin(String code) throws JsonProcessingException {
        // 1. "?????? ??????"??? "????????? ??????" ??????
        String kakaoAccessToken = getAccessToken(code);
        // 2. ???????????? ????????? API ??????
        KakaoUserInfoDto kakaoUserInfo = getKakaoUserInfo(kakaoAccessToken);

        // DB ??? ????????? Kakao Id ??? ????????? ??????
        Long kakaoId = kakaoUserInfo.getKakaoId();
        User kakaoUser = userRepository.findByKakaoId(kakaoId)
                .orElse(null);
        if (kakaoUser == null) {
        // role: ?????? ?????????
            UserRole role = UserRole.ROLE_USER;
            RegisterType registerType = RegisterType.SOCIAL;
            kakaoUser = new User(role, registerType ,kakaoId, getSocialRandomValue("kakaoUser"));

            User user = userRepository.save(kakaoUser);

            UserImage userImage = userImageRepository.save(new UserImage(user, kakaoUserInfo));

            user.updateProfileImage(userImage);
        }
        return createToken(new UserRequestDto(kakaoUser.getUsername()));
    }


    public String getSocialRandomValue(String provider) {
        while (true) {
            String value = provider + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 5);
            if (!userRepository.existsByUsername(value) && !userRepository.existsByNickname(value)) {
                return value;
            }
        }
    }

    public SocialTokenDto createToken(UserRequestDto userRequestDto){
        SocialTokenDto tokenDto = tokenProvider.socialLoginTokenCreate(userRequestDto);

        tokenDto.setUsername(userRequestDto.getUsername());

        User user = userRepository.findByUsername(userRequestDto.getUsername()).orElse(null);
        assert user != null;
        tokenDto.setNickname(user.getNickname());

        // 4. RefreshToken ??????
        RefreshToken refreshToken = RefreshToken.builder()
                .key(userRequestDto.getUsername())
                .value(tokenDto.getRefreshToken())
                .build();

        refreshTokenRepository.save(refreshToken);

        // 5. ?????? ??????
        return tokenDto;
    }

    //kakao ?????? code ????????? access token ??????
    private String getAccessToken(String code) throws JsonProcessingException {
        // HTTP Header ??????
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

// HTTP Body ??????
            StringBuilder body = new StringBuilder();
            body.append("grant_type=authorization_code&client_id=7ed074dd8ee05fafa99735fba28a41d2&redirect_uri=https://anyzoo.co.kr/oauth&code=" + code + "&client_secret=28ibusk6KsYMwzHk2MyKow5ed5wV8j8l");
//https://anyzoo.co.kr/oauth

// HTTP ?????? ?????????
            HttpEntity<String> kakaoTokenRequest =
                    new HttpEntity<String>(body.toString(), headers);
            RestTemplate rt = new RestTemplate();
            ResponseEntity<String> response = rt.exchange(
                    "https://kauth.kakao.com/oauth/token",
                    HttpMethod.POST,
                    kakaoTokenRequest,
                    String.class
            );

// HTTP ?????? (JSON) -> ????????? ?????? ??????
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText();
    }

    //kakao ???????????? ?????????
    private KakaoUserInfoDto getKakaoUserInfo(String accessToken) throws JsonProcessingException {

        HttpHeaders headers = new HttpHeaders();

        // HTTP Header ??????
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

// HTTP ?????? ?????????
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String>response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoUserInfoRequest,
                String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long id = jsonNode.get("id").asLong();
        String profile_image = jsonNode.get("kakao_account").get("profile").get("profile_image_url").asText();

        System.out.println("????????? ????????? ??????: " + id + ", " + profile_image);
        return new KakaoUserInfoDto(id, profile_image);
    }

    //----------------------------?????? ?????? ?????? ??????-------------------------------
    //???????????? ???????????? ??????
    @Transactional
    public void changePassword(NewPasswordDto newPasswordDto){
        User user = userRepository
                .findByPhoneNumber(newPasswordDto.getPhoneNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.OK_BUT_NO_USER));
        String newPassword = passwordEncoder.encode(newPasswordDto.getNewPassword());
        user.changePassword(newPassword);
    }

    //???????????? Username(email) ??????
    @Transactional
    public String findLostEmail(String phoneNumber){
            User user = userRepository
                    .findByPhoneNumber(phoneNumber)
                    .orElseGet(() -> null);
                    //.orElseThrow(() -> new IllegalArgumentException("????????? ???????????? ????????????"));
                    //.orElseThrow(() -> new CustomException(ErrorCode.OK_BUT_NO_USER));
            if(!Optional.ofNullable(user).isPresent()) {
                return "???????????? ?????? ?????? ?????? ?????????";
            }
            String[] usernameSplit = user.getUsername().split("@");
            if (usernameSplit[0].length() <= 2) {
                return user.getUsername().charAt(0) + "*****@" + usernameSplit[1];
            } else {
                return user.getUsername().substring(0, 2) + "****@" + usernameSplit[1];
            }
    }

    @Transactional
    public Map<String, Object> editUserInfo(UserEditDto userEditDto) {
        User user = getUser();
        if(Optional.ofNullable(userEditDto.getNickname()).isPresent()) {
            user.editUserNickname(userEditDto.getNickname());
        }
        if(Optional.ofNullable(userEditDto.getPhoneNumber()).isPresent()){
            user.editUserPhoneNumber(userEditDto.getPhoneNumber());
        }
        if(Optional.ofNullable(userEditDto.getPassword()).isPresent()){
            user.changePassword(passwordEncoder.encode(userEditDto.getPassword()));
        }
        //        if(Optional.ofNullable(userEditDto.getNewPassword()).isPresent()){
//            if(passwordEncoder.matches(userEditDto.getOldPassword(), user.getPassword())){
//                user.changePassword(passwordEncoder.encode(userEditDto.getNewPassword()));
//            }else{
//                throw new CustomException(ErrorCode.PASSWORDS_NOT_MATCH);
//            }
//        }
        Map<String, Object> data = new HashMap<>();
        data.put("nickname", new UserInfo(user));
        return data;
    }

    @Transactional
    public Map<String, Object> editUserImage(ProfileImageDto profileImageDto){
        User user = getUser();

        UserImage userImage;
        if (profileImageDto.getUserImage()==null){
            userImageRepository.save(userImage = new UserImage());
        } else {
            userImage = userImageRepository.findById(profileImageDto.getUserImage()).orElseThrow(()->new CustomException(ErrorCode.Image_NOT_FOUND));
        }
        user.updateProfileImage(userImage);
        userImage.updateToUser(user);

        Map<String, Object> data = new HashMap<>();
        data.put("nickname", new UserInfo(user));
        return data;
    }

    @Transactional
    public TokenDto reissue(TokenRequestDto tokenRequestDto){
        // ????????? ????????? ???????????? ??????
        UserValidator.validateRefreshTokenReissue(tokenRequestDto);

        // 1. Refresh Token ????????? ??????
        if (!tokenProvider.validateToken(tokenRequestDto.getRefreshToken())) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // 2. Access Token ?????? Member ID ????????????
        Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FOUND_USER_INFO)
        );
        // 3. ??????????????? Member ID ??? ???????????? Refresh Token ??? ?????????
        RefreshToken refreshToken = refreshTokenRepository.findByKey(user.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 4. Refresh Token ??????????????? ??????
        if (!refreshToken.getValue().equals(tokenRequestDto.getRefreshToken())) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_MATCH);
        }

        // 5. ????????? ?????? ??????
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 6. ????????? ?????? ????????????
        RefreshToken newRefreshToken = refreshToken.updateValue(tokenDto.getRefreshToken());
        refreshTokenRepository.save(newRefreshToken);

        // ?????? ??????
        return tokenDto;
    }


    //----------------------------?????? ?????? ??????-------------------------------
    public User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName()).orElseThrow(
                () -> new UsernameNotFoundException("???????????? ?????? ???????????????")
        );
    }

    public User getUserObject(String username) {
        return userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("???????????? ?????? ???????????????")
        );
    }
}
