package kr.eolmago.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.chat.ChatMessageRepository;
import kr.eolmago.service.chat.ChatService;
import kr.eolmago.service.chat.ChatStreamPublisher;
import kr.eolmago.service.chat.exception.ChatException;
import kr.eolmago.service.chat.validation.ChatValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

	private ChatRoomServiceTestDoubles doubles;
	private ChatService sut;

	@BeforeEach
	void setUp() {
		doubles = ChatRoomServiceTestDoubles.create();

		// createOrGetRoom 테스트에서는 아래 두 의존성은 사용되지 않으므로 mock이면 충분
		ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
		ChatStreamPublisher chatStreamPublisher = mock(ChatStreamPublisher.class);

		// Validator는 의존성 없으니 실객체
		ChatValidator chatValidator = new ChatValidator();

		sut = new ChatService(
			doubles.chatRoomRepository,
			chatMessageRepository,
			doubles.auctionRepository,
			doubles.userRepository,
			chatStreamPublisher,
			chatValidator
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
	@DisplayName("방이 없고 buyer가 없으면 requester를 buyer로 간주해 방 생성")
	void buyerNull_createsRoom() {
		// given
		ChatRoomScenario s = given()
			.auctionBuyerNull()
			.requesterUserExists();

		// when
		Long roomId = sut.createOrGetRoom(s.auctionId, s.requesterId);

		// then
		ChatRoom saved = doubles.findRoomOrThrow(roomId);
		assertThat(saved.getSeller().getUserId()).isEqualTo(s.sellerId);
		assertThat(saved.getBuyer().getUserId()).isEqualTo(s.requesterId);
	}

	@Test
	@DisplayName("buyer가 없고 requester가 seller면 예외(CH203)")
	void sellerCannotCreateAsBuyer_throws() {
		// given
		ChatRoomScenario s = given().auctionBuyerNull();

		// when / then
		assertThatThrownBy(() -> sut.createOrGetRoom(s.auctionId, s.sellerId))
			.isInstanceOf(ChatException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CHAT_SELLER_CANNOT_CREATE_AS_BUYER);
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
			.auctionBuyerNull()
			.requesterUserExists()
			.saveRaceOccurs();

		// when
		Long roomId = sut.createOrGetRoom(s.auctionId, s.requesterId);

		// then
		ChatRoom room = doubles.findRoomOrThrow(roomId);
		assertThat(room.getBuyer().getUserId()).isEqualTo(s.requesterId);
	}
}
