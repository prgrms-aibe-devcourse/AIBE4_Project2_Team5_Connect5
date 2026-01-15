package kr.eolmago.repository.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.chat.ChatRoomType;

public interface ChatRoomRepositoryCustom {

    List<ChatRoomSummaryProjection> findMyRoomSummariesByType(UUID userId, ChatRoomType roomType);

    int markRead(Long roomId, UUID userId, Long messageId);

    Optional<ChatRoom> findRoomViewById(Long roomId);
}
