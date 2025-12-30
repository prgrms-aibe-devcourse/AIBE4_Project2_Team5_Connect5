package kr.eolmago.domain.entity.report.enums;

// 경미한 신고 -> WARN
// 1회: SUSPEND_7D
// 2회: SUSPEND_30D
// 3회: BAN
public enum ReportAction {
    NONE, // 조치 없음
    WARN, // 경고
    SUSPEND_7D, // 7일 정지
    SUSPEND_30D, // 30일 정지
    BAN // 영구 정지
}