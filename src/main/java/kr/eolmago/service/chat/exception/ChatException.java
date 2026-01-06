package kr.eolmago.service.chat.exception;

import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;

public class ChatException extends BusinessException {
	public ChatException(ErrorCode errorCode) {
		super(errorCode);
	}
	public ChatException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
