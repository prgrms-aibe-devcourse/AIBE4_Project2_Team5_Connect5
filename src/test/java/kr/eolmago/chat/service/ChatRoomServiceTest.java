package kr.eolmago.chat.service;

import static org.assertj.core.api.Assertions.*;

import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.service.chat.ChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatRoomServiceTest {

	private ChatRoomServiceTestDoubles doubles;
	private ChatRoomService sut;

	@BeforeEach
	void setUp() {
		doubles = ChatRoomServiceTestDoubles.create();
		sut = new ChatRoomService(doubles.chatRoomRepository, doubles.auctionRepository, doubles.userRepository);
	}

	private ChatRoomScenario given() {
		return ChatRoomScenario.given(doubles);
	}

	@Test
	@DisplayName("기존 방이 있으면 참여자 검증 후 기존 roomId 반환")
	void givenExistingRoom_whenRequesterParticipates_thenReturnExistingRoomId() {
		// Given
		ChatRoomScenario s = given()
			.auctionWithBuyer()
			.roomAlreadyExists();

		// When
		Long roomId = sut.createOrGetRoom(s.auctionId, s.sellerId);

		// Then
		assertThat(roomId).isEqualTo(s.existingRoomId());
	}

	@Test
	@DisplayName("방이 없고 buyer가 없으면 requester를 buyer로 간주해 방 생성")
	void givenBuyerNullAndNoRoom_whenRequesterIsNotSeller_thenCreateRoomWithRequesterAsBuyer() {
		// Given
		ChatRoomScenario s = given()
			.auctionBuyerNull()
			.requesterUserExists();

		// When
		Long roomId = sut.createOrGetRoom(s.auctionId, s.requesterId);

		// Then
		ChatRoom saved = doubles.findRoomOrThrow(roomId);
		assertThat(saved.getSeller().getUserId()).isEqualTo(s.sellerId);
		assertThat(saved.getBuyer().getUserId()).isEqualTo(s.requesterId);
	}

	@Test
	@DisplayName("방이 없고 buyer가 없을 때 seller가 requester면 예외")
	void givenBuyerNullAndNoRoom_whenRequesterIsSeller_thenThrow() {
		// Given
		ChatRoomScenario s = given()
			.auctionBuyerNull();

		// When / Then
		assertThatThrownBy(() -> sut.createOrGetRoom(s.auctionId, s.sellerId))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("seller cannot create room as buyer");
	}

	@Test
	@DisplayName("buyer가 존재할 때 참여자가 아니면 예외")
	void givenBuyerExists_whenRequesterIsNotParticipant_thenThrow() {
		// Given
		ChatRoomScenario s = given()
			.auctionWithBuyer();

		// When / Then
		assertThatThrownBy(() -> sut.createOrGetRoom(s.auctionId, s.otherId))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("no permission for this auction chat");
	}

	@Test
	@DisplayName("save 경합(DataIntegrityViolationException) 발생 시 재조회로 roomId 반환")
	void givenSaveRace_whenCreateRoom_thenReturnRoomIdFromRefetch() {
		// Given
		ChatRoomScenario s = given()
			.auctionBuyerNull()
			.requesterUserExists()
			.saveRaceOccurs();

		// When
		Long roomId = sut.createOrGetRoom(s.auctionId, s.requesterId);

		// Then
		ChatRoom room = doubles.findRoomOrThrow(roomId);
		assertThat(room.getBuyer().getUserId()).isEqualTo(s.requesterId);
	}
}
