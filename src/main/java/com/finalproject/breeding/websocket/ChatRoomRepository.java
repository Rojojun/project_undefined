package com.finalproject.breeding.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Repository
public class ChatRoomRepository {
    // 채팅방에 발행되는 메시지를 처리할 Listner
    private final RedisMessageListenerContainer redisMessageListener;
    // 구독 처리 서비스
    private final RedisSubscriber redisSubscriber;
    // Redis
    private static final String CHAT_ROOMS = "CHAT_ROOM";
    private final RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, ChatRoom> opsHashChatRoom;
    // 채팅방의 대화 메시지를 발행하기 위한 redis topic 정보. roomId로 찾을 수 있도록함
    private Map<String, ChannelTopic> topics;

    @PostConstruct
    private void init(){
        opsHashChatRoom = redisTemplate.opsForHash();
        topics = new HashMap<>();
    }
    public List<ChatRoom> findAllRoom(){
        return opsHashChatRoom.values(CHAT_ROOMS);
    }
    public ChatRoom findRoomById(String id){
        return opsHashChatRoom.get(CHAT_ROOMS, id);
    }

    // 채팅방 생성
    public ChatRoom createChatRoom(String name){
        ChatRoom chatRoom = ChatRoom.create(name);
        opsHashChatRoom.put(CHAT_ROOMS, chatRoom.getRoomId(), chatRoom);
        return chatRoom;
    }

    // 채팅방 입장
    public void enterChatRoom(String roomId){
        ChannelTopic topic = topics.get(roomId);
        if (topic == null){
            topic = new ChannelTopic(roomId);
            redisMessageListener.addMessageListener(redisSubscriber, topic);
            topics.put(roomId, topic);
        }
    }
    public ChannelTopic getTopic(String roomId){
        return topics.get(roomId);
    }
}