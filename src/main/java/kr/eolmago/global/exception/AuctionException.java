package kr.eolmago.global.exception;

/**
 * 경매 관련 비즈니스 예외
 */
public class AuctionException extends RuntimeException {

    private final ErrorCode errorCode;

    public AuctionException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AuctionException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
