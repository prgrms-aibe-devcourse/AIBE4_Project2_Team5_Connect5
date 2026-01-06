package kr.eolmago.domain.entity.notification.enums;

public enum NotificationType {
    AUCTION_ENDED, // 경매 종료 알림
    BID_OUTBID, // 입찰가 갱신/추월 알림
    DEAL_CONFIRMED, // 거래 확정 관련 알림
    REPORT_RECEIVED, // 신고 접수/처리 관련 알림
    CHAT_MESSAGE, // 채팅 메시지 알림
    WELCOME //알림 테스트용
}