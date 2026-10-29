package com.hang.shiying.core;

public final class UserFacingException extends Exception {
    public final ErrorCode code;

    public UserFacingException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public UserFacingException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
