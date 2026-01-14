package kr.eolmago.domain.entity.notification.enums;

public enum NotificationType {
    // 경매
    AUCTION_PUBLISHED,        // 경매 등록 완료(게시)
    AUCTION_ENDED,            // 경매 종료(기존)
    AUCTION_SOLD,             // 판매자: 낙찰 확정
    AUCTION_WON,              // 구매자: 낙찰
    AUCTION_UNSOLD,           // 판매자: 유찰
    AUCTION_CANCELED,         // 판매자: 경매 취소
    AUCTION_ENDED_WATCHING,   // 관심/입찰자: 마감 결과(관전용)
    BID_ACCEPTED,             // 구매자: 입찰 성공 처리 결과
    BID_OUTBID,               // 구매자: 최고가에서 밀림(기존)

    // 거래
    DEAL_CONFIRMED,           // 거래 확정(기존)
    DEAL_COMPLETED,           // 거래 완료(양쪽)
    DEAL_EXPIRING_SOON,       // 거래 만료 임박(양쪽)

    // 신고
    REPORT_RECEIVED,          // 신고 접수(기존)
    REPORT_ACTION_COMPLETED,  // 조치 완료 -> 신고자
    REPORT_SUSPENDED,         // 기간 정지 -> 피신고자
    REPORT_REJECTED,          // 기각 -> 신고자

    // 유저
    PHONE_VERIFIED,           // 전화번호 인증 완료(GUEST -> USER)
    WELCOME,                  // 환영(기존)

    // 채팅
    CHAT_MESSAGE,             // 채팅 메시지(기존)
    CHAT_ROOM_CREATED,         // 마감 후 채팅방 생성(양쪽)
    FAVORITE_ADDED
}
