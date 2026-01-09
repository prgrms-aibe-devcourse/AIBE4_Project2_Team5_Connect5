package kr.eolmago.global.util;

import kr.eolmago.domain.entity.auction.enums.ItemCategory;

/**
 * ItemCategory Enum 유틸리티
 * - Enum ↔ 한글 표시명 변환
 */
public class ItemCategoryUtils {

    /**
     * Enum → 한글 표시명
     */
    public static String getDisplayName(ItemCategory category) {
        if (category == null) {
            return "전체";
        }

        return switch (category) {
            case PHONE -> "핸드폰";
            case TABLET -> "태블릿";
        };
    }

    /**
     * 한글 표시명 → Enum (필요시 사용)
     */
    public static ItemCategory fromDisplayName(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return null;
        }

        return switch (displayName) {
            case "핸드폰" -> ItemCategory.PHONE;
            case "태블릿" -> ItemCategory.TABLET;
            default -> null;
        };
    }
}
