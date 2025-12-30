package kr.eolmago.domain.entity.report.enums;

public enum ReportReason {
    FRAUD_SUSPECT,        // 사기 의심
    ITEM_NOT_AS_DESCRIBED,// 설명/사진 불일치
    ABUSIVE_LANGUAGE,     // 욕설/비매너
    SPAM_AD,              // 광고/도배
    ILLEGAL_ITEM,         // 불법/금지 품목
    COUNTERFEIT,          // 가품 의심
    PERSONAL_INFO,        // 개인정보 노출
    OTHER                 // 기타
}
