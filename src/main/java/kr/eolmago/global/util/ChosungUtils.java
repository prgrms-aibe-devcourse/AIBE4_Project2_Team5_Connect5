package kr.eolmago.global.util;

import java.util.regex.Pattern;

/**
 * 한글 초성 관련 유틸리티
 *  - 초성 여부 판단
 *  - 한글 여부 판단
 *  - 검색어 타입 분석
 *
 * 사용처:
 * - AuctionSearchService (검색 타입 판단)
 * - SearchKeywordService (자동완성)
 * - Repository (쿼리 분기)
 *
 * 설계 원칙:
 * - 모든 초성 관련 로직을 한 곳에 집중
 * - 중복 코드 제거
 * - static 메서드로 유틸리티 제공
 */
public class ChosungUtils {

    /**
     * 초성 문자 범위
     * - 유니코드: 12593(ㄱ) ~ 12622(ㅎ)
     * - 총 19개: ㄱ ㄲ ㄴ ㄷ ㄸ ㄹ ㅁ ㅂ ㅃ ㅅ ㅆ ㅇ ㅈ ㅉ ㅊ ㅋ ㅌ ㅍ ㅎ
     */
    private static final int CHOSUNG_START = 0x3131; // 12593 (ㄱ)
    private static final int CHOSUNG_END = 0x314E;   // 12622 (ㅎ)

    /**
     * 완성형 한글 범위
     * - 유니코드: 44032(가) ~ 55203(힣)
     */
    private static final int HANGUL_START = 0xAC00; // 44032 (가)
    private static final int HANGUL_END = 0xD7A3;   // 55203 (힣)

    /**
     * 초성만으로 이루어진 문자열 패턴
     * - ㄱ-ㅎ 범위만 매칭
     * - 숫자, 영문, 공백 포함 시 false
     */
    private static final Pattern CHOSUNG_ONLY_PATTERN = Pattern.compile("^[ㄱ-ㅎ]+$");

    /**
     * 초성 포함 문자열 패턴 (부분 매칭)
     * - 초성이 하나라도 포함되어 있으면 true
     */
    private static final Pattern CONTAINS_CHOSUNG_PATTERN = Pattern.compile("[ㄱ-ㅎ]");

    /**
     * 완성형 한글 포함 여부 패턴
     * - 가-힣 범위 매칭
     */
    private static final Pattern CONTAINS_HANGUL_PATTERN = Pattern.compile("[가-힣]");

    /**
     * 초성만으로 이루어진 문자열인지 판단
     *
     * @param text 검사할 문자열
     * @return 초성만 있으면 true, 그 외 false
     */
    public static boolean isChosungOnly(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return CHOSUNG_ONLY_PATTERN.matcher(text).matches();
    }

    /**
     * 초성이 포함된 문자열인지 판단
     *
     * 차이점:
     * - isChosungOnly(): 초성"만" 있는지
     * - containsChosung(): 초성이 "하나라도" 있는지
     *
     * 사용 예시:
     * ChosungUtils.containsChosung("ㅇㅍ")      // true
     * ChosungUtils.containsChosung("ㅇㅍ14")    // true (초성 + 숫자)
     * ChosungUtils.containsChosung("아이ㅍ")    // true (한글 + 초성)
     * ChosungUtils.containsChosung("아이폰")    // false
     * ChosungUtils.containsChosung("iPhone")   // false
     *
     * @param text 검사할 문자열
     * @return 초성이 하나라도 포함되면 true
     */
    public static boolean containsChosung(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return CONTAINS_CHOSUNG_PATTERN.matcher(text).find();
    }

    /**
     * 완성형 한글이 포함된 문자열인지 판단
     *
     * ex)
     * ChosungUtils.containsHangul("아이폰14")   // true
     * ChosungUtils.containsHangul("ㅇㅍ")       // false (초성만)
     *
     * @param text 검사할 문자열
     * @return 완성형 한글이 포함되면 true
     */
    public static boolean containsHangul(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return CONTAINS_HANGUL_PATTERN.matcher(text).find();
    }

    /**
     * 문자가 초성인지 판단
     *
     * @param c 검사할 문자
     * @return 초성이면 true
     */
    public static boolean isChosungChar(char c) {
        return c >= CHOSUNG_START && c <= CHOSUNG_END;
    }

    /**
     * 문자가 완성형 한글인지 판단
     *
     * @param c 검사할 문자
     * @return 완성형 한글이면 true
     */
    public static boolean isHangulChar(char c) {
        return c >= HANGUL_START && c <= HANGUL_END;
    }

    /**
     * 검색어 타입 판단
     *
     * 반환값:
     * - "CHOSUNG": 초성만 (예: "ㅇㅍ")
     * - "HANGUL": 한글 포함 (예: "아이폰", "갤럭시S24")
     * - "MIXED": 초성 + 한글 혼합 (예: "ㅇㅍ폰")
     * - "OTHER": 영문/숫자만 (예: "iPhone", "14")
     *
     * @param text 검사할 문자열
     * @return 키워드 타입 ("CHOSUNG", "HANGUL", "MIXED", "OTHER")
     */
    public static String getKeywordType(String text) {
        if (text == null || text.isEmpty()) {
            return "OTHER";
        }

        boolean hasChosung = containsChosung(text);
        boolean hasHangul = containsHangul(text);

        if (hasChosung && !hasHangul) {
            return "CHOSUNG";  // 초성만
        } else if (!hasChosung && hasHangul) {
            return "HANGUL";   // 한글만 (또는 한글 + 영문/숫자)
        } else if (hasChosung && hasHangul) {
            return "MIXED";    // 초성 + 한글 혼합
        } else {
            return "OTHER";    // 영문/숫자만
        }
    }


}
