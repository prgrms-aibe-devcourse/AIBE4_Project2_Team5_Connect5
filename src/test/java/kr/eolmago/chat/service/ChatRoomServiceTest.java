package kr.eolmago.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.chat.ChatMessageRepository;
import kr.eolmago.service.chat.ChatService;
import kr.eolmago.service.chat.ChatStreamPublisher;
import kr.eolmago.service.chat.ChatSystemUserProvider;
import kr.eolmago.service.chat.exception.ChatException;
import kr.eolmago.service.chat.validation.ChatValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatRoomServiceTest {

	private ChatRoomServiceTestDoubles doubles;
	private ChatService sut;

	@BeforeEach
	void setUp() {
		doubles = ChatRoomServiceTestDoubles.create();

		ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
		ChatStreamPublisher chatStreamPublisher = mock(ChatStreamPublisher.class);
		ChatSystemUserProvider systemUserProvider = mock(ChatSystemUserProvider.class);

		ChatValidator chatValidator = new ChatValidator();

		sut = new ChatService(
			doubles.chatRoomRepository,
			chatMessageRepository,
			doubles.auctionRepository,
			doubles.userRepository,
			chatStreamPublisher,
			chatValidator,
			systemUserProvider
		);
	}

	private ChatRoomScenario given() {
		return ChatRoomScenario.given(doubles);
	}

	@Test
	@DisplayName("기존 방이 있으면 참여자 검증 후 기존 roomId 반환")
	void existingRoom_returnsId() {
		// given
		ChatRoomScenario s = given()
			.auctionWithBuyer()
			.roomAlreadyExists();

		// when
		Long roomId = sut.createOrGetRoom(s.auctionId, s.sellerId);

		// then
		assertThat(roomId).isEqualTo(s.existingRoomId());
	}

	@Test
	@DisplayName("방이 없고 buyer가 있으면 참여자 요청으로 새 채팅방 생성")
	void noRoom_createsNewRoom_whenParticipant() {
		// given
		ChatRoomScenario s = given().auctionWithBuyer();

		// when
		Long roomId = sut.createOrGetRoom(s.auctionId, s.buyerId);

		// then
		ChatRoom saved = doubles.findRoomOrThrow(roomId);
		assertThat(saved.getSeller().getUserId()).isEqualTo(s.sellerId);
		assertThat(saved.getBuyer().getUserId()).isEqualTo(s.buyerId);
	}

	@Test
	@DisplayName("buyer가 없으면 채팅방 생성 불가 예외(CH204: NOT_AVAILABLE_YET)")
	void buyerNull_throwsNotAvailableYet() {
		// given
		ChatRoomScenario s = given().auctionBuyerNull();

		// when / then
		assertThatThrownBy(() -> sut.createOrGetRoom(s.auctionId, s.sellerId))
			.isInstanceOf(ChatException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CHAT_ROOM_NOT_AVAILABLE_YET);
	}

	@Test
	@DisplayName("buyer가 존재할 때 참여자가 아니면 예외(CH202)")
	void nonParticipant_throws() {
		// given
		ChatRoomScenario s = given().auctionWithBuyer();

		// when / then
		assertThatThrownBy(() -> sut.createOrGetRoom(s.auctionId, s.otherId))
			.isInstanceOf(ChatException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CHAT_FORBIDDEN_AUCTION);
	}

	@Test
	@DisplayName("save 경합 발생 시 재조회로 roomId 반환")
	void race_returnsIdFromRefetch() {
		// given
		ChatRoomScenario s = given()
			.auctionWithBuyer()
			.saveRaceOccurs();

		// when
		Long roomId = sut.createOrGetRoom(s.auctionId, s.buyerId);

		// then
		ChatRoom room = doubles.findRoomOrThrow(roomId);
		assertThat(room.getSeller().getUserId()).isEqualTo(s.sellerId);
		assertThat(room.getBuyer().getUserId()).isEqualTo(s.buyerId);
	}
}
