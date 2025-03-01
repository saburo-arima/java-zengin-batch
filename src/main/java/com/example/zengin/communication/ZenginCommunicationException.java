package com.example.zengin.communication;

/**
 * 全銀通信処理における例外クラス
 * 通信エラーや全銀プロトコル処理中のエラーを表します
 */
public class ZenginCommunicationException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * エラーコード
     */
    private final String errorCode;
    
    /**
     * コンストラクタ
     * 
     * @param message エラーメッセージ
     */
    public ZenginCommunicationException(String message) {
        super(message);
        this.errorCode = "E000"; // デフォルトエラーコード
    }
    
    /**
     * コンストラクタ
     * 
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public ZenginCommunicationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "E000"; // デフォルトエラーコード
    }
    
    /**
     * コンストラクタ
     * 
     * @param message エラーメッセージ
     * @param errorCode エラーコード
     */
    public ZenginCommunicationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * コンストラクタ
     * 
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     * @param errorCode エラーコード
     */
    public ZenginCommunicationException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * エラーコードを取得します
     * 
     * @return エラーコード
     */
    public String getErrorCode() {
        return errorCode;
    }
} 