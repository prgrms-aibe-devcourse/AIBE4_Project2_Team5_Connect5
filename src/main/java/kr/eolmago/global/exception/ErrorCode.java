package kr.eolmago.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 내부 오류가 발생했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U002", "인증되지 않은 사용자입니다."),
    USER_FORBIDDEN(HttpStatus.FORBIDDEN, "U003", "권한이 없습니다."),
    USER_BANNED(HttpStatus.FORBIDDEN, "U004", "영구 정지된 이용자입니다."),
    USER_PROFILE_IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "U005", "프로필 이미지는 2MB 이하여야 합니다."),
    USER_PROFILE_IMAGE_RESOLUTION_EXCEEDED(HttpStatus.BAD_REQUEST, "U006", "프로필 이미지는 400x400 해상도 이하여야 합니다."),
    USER_PROFILE_IMAGE_INVALID_EXTENSION(HttpStatus.BAD_REQUEST, "U007", "프로필 이미지는 jpg, jpeg, png, webp 확장자만 가능합니다."),

    // Auction
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "경매를 찾을 수 없습니다."),
    AUCTION_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "A002", "이미 시작된 경매입니다."),
    AUCTION_NOT_DRAFT(HttpStatus.BAD_REQUEST, "A003", "임시 저장 상태의 경매가 아닙니다."),
    AUCTION_NOT_LIVE(HttpStatus.BAD_REQUEST, "A004", "진행 중인 경매가 아닙니다."),
    AUCTION_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "A005", "이미 종료된 경매입니다."),
    AUCTION_HAS_BIDS(HttpStatus.BAD_REQUEST, "A006", "입찰이 존재하는 경매는 중지할 수 없습니다."),
    AUCTION_UNAUTHORIZED(HttpStatus.FORBIDDEN, "A007", "본인의 경매만 접근할 수 있습니다."),
    SELLER_CANNOT_BID(HttpStatus.BAD_REQUEST, "A008", "본인의 경매에는 입찰할 수 없습니다."),
    AUCTION_SELLER_ONLY(HttpStatus.FORBIDDEN, "A009", "판매자만 수행할 수 있습니다."),
    AUCTION_BUYER_ONLY(HttpStatus.FORBIDDEN, "A010", "구매자만 수행할 수 있습니다."),
    AUCTION_PARTICIPANT_ONLY(HttpStatus.FORBIDDEN, "A011", "해당 경매 참여자만 접근할 수 있습니다."),
    AUCTION_PUBLISH_ONLY_DRAFT(HttpStatus.BAD_REQUEST, "A012", "임시저장 상태의 경매만 게시할 수 있습니다."),
    AUCTION_DELETE_ONLY_DRAFT(HttpStatus.BAD_REQUEST, "A013", "임시저장 상태의 경매만 삭제할 수 있습니다."),
    AUCTION_INVALID_DURATION(HttpStatus.BAD_REQUEST, "A014", "경매 기간이 올바르지 않습니다."),
    AUCTION_NOT_ENDED(HttpStatus.CONFLICT, "A015", "종료되지 않은 경매입니다."),
    AUCTION_ALREADY_CLOSED(HttpStatus.CONFLICT, "A016", "이미 마감된 경매입니다."),
    AUCTION_NOT_UNSOLD(HttpStatus.CONFLICT, "A017", "유찰 상태의 경매가 아닙니다."),
    AUCTION_CANNOT_STOP(HttpStatus.CONFLICT, "A018", "경매를 중지할 수 없습니다."),
    AUCTION_HAS_ACTIVE_BIDS(HttpStatus.CONFLICT, "A019", "입찰이 존재하는 경매는 중지할 수 없습니다."),


    // Bid
    BID_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "입찰을 찾을 수 없습니다."),
    BID_AMOUNT_TOO_LOW(HttpStatus.BAD_REQUEST, "B002", "최소 입찰가보다 낮은 금액입니다."),
    BID_LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "B003", "현재 입찰이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),
    BID_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "B004", "이미 입찰한 경매입니다."),
    BID_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "B005", "입찰 금액이 최소 입찰가보다 낮습니다."),
    BID_INVALID_INCREMENT(HttpStatus.BAD_REQUEST, "B006", "입찰 금액이 입찰 단위에 맞지 않습니다."),
    BID_IDEMPOTENCY_REQUIRED(HttpStatus.BAD_REQUEST, "B007", "요청 식별값이 누락되었습니다."),
    BID_IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "B008", "요청 식별값이 기존 요청과 충돌합니다."),
    BID_AMOUNT_EXCEEDS_LIMIT(HttpStatus.BAD_REQUEST, "B009", "입찰 금액이 상한선을 초과했습니다."),
    AUCTION_LOCK_BUSY(HttpStatus.CONFLICT, "B010", "요청이 많아 잠시 후 다시 시도해주세요."),

    // Chat
    CHAT_AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "CH001", "로그인이 필요합니다."),
    CHAT_INVALID_AUTH(HttpStatus.UNAUTHORIZED, "CH002", "인증 정보가 올바르지 않습니다."),

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CH101", "채팅방을 찾을 수 없습니다."),

    CHAT_FORBIDDEN_ROOM(HttpStatus.FORBIDDEN, "CH201", "해당 채팅방에 접근 권한이 없습니다."),
    CHAT_FORBIDDEN_AUCTION(HttpStatus.FORBIDDEN, "CH202", "해당 경매 채팅에 접근 권한이 없습니다."),
    CHAT_SELLER_CANNOT_CREATE_AS_BUYER(HttpStatus.BAD_REQUEST, "CH203", "판매자는 구매자 권한으로 채팅방을 생성할 수 없습니다."),

    CHAT_INVALID_SEND_REQUEST(HttpStatus.BAD_REQUEST, "CH301", "채팅 전송 요청이 올바르지 않습니다."),

    // Deal
    DEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "거래를 찾을 수 없습니다."),
    DEAL_UNAUTHORIZED(HttpStatus.FORBIDDEN, "D002", "해당 거래에 접근 권한이 없습니다."),
    DEAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "D003", "이미 해당 경매로 거래가 생성되었습니다."),
    AUCTION_ID_MISMATCH(HttpStatus.BAD_REQUEST, "D004", "경매 ID가 일치하지 않습니다."),
    DEAL_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "D005", "이미 확정된 거래입니다.");



    private final HttpStatus status;
    private final String code;
    private final String message;
}
