package com.finalproject.breeding.chat.config;

import com.finalproject.breeding.chat.model.ChatRoom;
import com.finalproject.breeding.chat.model.ChatRoomMessage;
import com.finalproject.breeding.chat.service.ChatRoomService;
import com.finalproject.breeding.chat.service.ChatService;
import com.finalproject.breeding.user.service.UserService;
import com.finalproject.breeding.user.token.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {
    private final TokenProvider tokenProvider;
    private final ChatRoomService chatRoomService;
    private final ChatService chatService;
    private final UserService userService;

    // Controller 에 가기 전에 이곳을 먼저 들리게 된다. 그것이 인터셉터의 역할.
    //    // HTTP의 Request / Response처럼
    //    // WebSocket은 message와 channel을 갖게된다.
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // accessor를 이용하면 내용에 패킷이 접근할 수 있게된다.
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // 접근했을때 COMMAND HEADER의 값을 확인 한다.
        // 만약 CONNECT라면 -> 초기 연결임
        if (StompCommand.CONNECT == accessor.getCommand()) { // websocket 연결요청
            // 토큰의 값만 확인 (로그인 여부를 확인하기 위함)
            // 헤더의 토큰값을 빼오기
            String jwtToken = accessor.getFirstNativeHeader("token");
            log.info("CONNECT {}", jwtToken);
            // Header의 jwt token 검증
            tokenProvider.validateToken(jwtToken);

            log.info("jwtToken validation 결과값={}", tokenProvider.validateToken(jwtToken));
        }

        //만약 COMMAND가 SUBSCRIBE 즉 메세지를 주고 받기 전 구독하는 것이라면
        else if (StompCommand.SUBSCRIBE == accessor.getCommand()) { // 채팅룸 구독요청
            // header정보에서 구독 destination 정보를 얻고, roomId를 추출한다.
            // roomId를 URL로 전송해주고 있어 추출 필요.
            // destination은 이렇게 생김 ->/sub/chat/room/{룸 아이디}
            String destination = Optional.ofNullable((String) message.getHeaders().get("simpDestination")).orElse("InvalidRoomId");
            log.info("message header 정보들={}", message.getHeaders());
            log.info("message destination은={}", destination);
            Long roomId = Long.parseLong(chatRoomService.getRoomId(destination));
            log.info("Long으로 Parsing된 roomId는={} [StompHandler_SUBSCRIBE]", roomId);

            // 채팅방에 들어온 클라이언트 sessionId를 roomId와 맵핑해 놓는다.(나중에 특정 세션이 어떤 채팅방에 들어가 있는지 알기 위함)
            // sessionId는 현재 들어와있는 유저를 확인하기 위함이다.
            // sessionId는 정상적으로 들어가고있음
            String sessionId = (String) message.getHeaders().get("simpSessionId");
            //sessionId와 roomId를 맵핑
            chatRoomService.setUserEnterInfo(sessionId, roomId);

            // 구독했다는 것은 처음 입장했다는 것이므로 입장 메시지를 발송한다.
            // 클라이언트 입장 메시지를 채팅방에 발송한다.(redis publish)
            String token = Optional.ofNullable(accessor.getFirstNativeHeader("token")).orElse("UnknownUser");
            log.info("token={} [StompHandler_SUBSCRIBE]", token);
            String username = tokenProvider.getUserPk(token);
            String nickname = userService.getUserObject(username).getNickname();
            chatService.sendChatMessage(
                    ChatRoomMessage.builder()
                            .type(ChatRoomMessage.MessageType.ENTER)
                            .roomId(roomId)
                            .sender(nickname)
                            .build());

            log.info("SUBSCRIBED nickname {}, roomId {}", nickname, roomId);
        }


        //룸을 이동하게 된다면 -> DISCONNET 시킨다 -> 채팅방을 나가는 것과 다름
        else if (StompCommand.DISCONNECT == accessor.getCommand()) { // Websocket 연결 종료

            // 연결이 종료된 클라이언트 sesssionId로 채팅방 id를 얻는다.
            String sessionId = (String) message.getHeaders().get("simpSessionId");
            //나갈떄 redis 맵에서 roomId와 sessionId의 매핑을 끊어줘야 하기때문에 roomId 찾고
            Long roomId = chatRoomService.getUserEnterRoomId(sessionId);
            log.info("HashOps에서 SessionId 기준으로 찾아온 roomId는={} [StompHandler_DISCONNECT]", roomId);

            // 퇴장한 클라이언트의 roomId 맵핑 정보를 삭제한다.
            chatRoomService.removeUserEnterInfo(sessionId);
            log.info("DISCONNECT SessionId {}, roomId {}", sessionId, roomId);
        }
        return message;
    }
}
