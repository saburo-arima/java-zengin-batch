package com.example.zengin.format;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 全銀フォーマットの電文を表すクラス
 * 全銀協標準通信プロトコルで使用される電文フォーマットを扱います
 */
public class ZenginMessage {
    
    // 全銀フォーマット定数
    private static final int HEADER_LENGTH = 80; // ヘッダレコード長
    private static final int DATA_LENGTH = 120; // データレコード長
    private static final int TRAILER_LENGTH = 80; // トレーラレコード長
    private static final Charset ZENGIN_CHARSET = StandardCharsets.UTF_8; // 文字コード（実際はJIS X 0208等）
    
    // 電文種別
    public enum MessageType {
        TRANSFER("01"), // 振込
        INQUIRY("02"),  // 照会
        RESPONSE("03"), // 応答
        NOTIFICATION("04"); // 通知
        
        private final String code;
        
        MessageType(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    // 電文ヘッダ情報
    private String senderId; // 送信者ID
    private String receiverId; // 受信者ID
    private LocalDateTime transmissionDateTime; // 送信日時
    private MessageType messageType; // 電文種別
    private String fileId; // ファイルID
    
    // 電文データ
    private byte[] headerRecord; // ヘッダレコード
    private byte[][] dataRecords; // データレコード配列
    private byte[] trailerRecord; // トレーラレコード
    
    /**
     * コンストラクタ
     * 
     * @param messageType 電文種別
     * @param senderId 送信者ID
     * @param receiverId 受信者ID
     */
    public ZenginMessage(MessageType messageType, String senderId, String receiverId) {
        this.messageType = messageType;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.transmissionDateTime = LocalDateTime.now();
        this.fileId = generateFileId();
    }
    
    /**
     * 電文をバイト配列に変換します
     * 
     * @return 全銀フォーマットのバイト配列
     */
    public byte[] toByteArray() {
        // ヘッダレコードの作成（未作成の場合）
        if (headerRecord == null) {
            createHeaderRecord();
        }
        
        // トレーラレコードの作成（未作成の場合）
        if (trailerRecord == null) {
            createTrailerRecord();
        }
        
        // 全体のサイズを計算
        int totalSize = HEADER_LENGTH;
        if (dataRecords != null) {
            totalSize += dataRecords.length * DATA_LENGTH;
        }
        totalSize += TRAILER_LENGTH;
        
        // 全体のバイト配列を作成
        byte[] message = new byte[totalSize];
        int offset = 0;
        
        // ヘッダレコードをコピー
        System.arraycopy(headerRecord, 0, message, offset, HEADER_LENGTH);
        offset += HEADER_LENGTH;
        
        // データレコードをコピー
        if (dataRecords != null) {
            for (byte[] dataRecord : dataRecords) {
                System.arraycopy(dataRecord, 0, message, offset, DATA_LENGTH);
                offset += DATA_LENGTH;
            }
        }
        
        // トレーラレコードをコピー
        System.arraycopy(trailerRecord, 0, message, offset, TRAILER_LENGTH);
        
        return message;
    }
    
    /**
     * バイト配列から電文を解析します
     * 
     * @param data 全銀フォーマットのバイト配列
     * @return 解析された電文オブジェクト
     * @throws IllegalArgumentException 不正なデータ形式の場合
     */
    public static ZenginMessage fromByteArray(byte[] data) throws IllegalArgumentException {
        if (data == null || data.length < HEADER_LENGTH + TRAILER_LENGTH) {
            throw new IllegalArgumentException("データサイズが不正です");
        }
        
        // ヘッダレコードを解析
        byte[] headerBytes = new byte[HEADER_LENGTH];
        System.arraycopy(data, 0, headerBytes, 0, HEADER_LENGTH);
        String headerStr = new String(headerBytes, ZENGIN_CHARSET);
        
        // 送信者ID、受信者ID、電文種別を取得
        String senderId = headerStr.substring(4, 14).trim();
        String receiverId = headerStr.substring(14, 24).trim();
        String messageTypeCode = headerStr.substring(24, 26);
        
        // 電文種別を特定
        MessageType messageType = null;
        for (MessageType type : MessageType.values()) {
            if (type.getCode().equals(messageTypeCode)) {
                messageType = type;
                break;
            }
        }
        
        if (messageType == null) {
            throw new IllegalArgumentException("不明な電文種別です: " + messageTypeCode);
        }
        
        // 電文オブジェクトを作成
        ZenginMessage message = new ZenginMessage(messageType, senderId, receiverId);
        
        // ヘッダレコードを設定
        message.headerRecord = headerBytes;
        
        // データレコード数を計算
        int dataRecordCount = (data.length - HEADER_LENGTH - TRAILER_LENGTH) / DATA_LENGTH;
        if (dataRecordCount > 0) {
            message.dataRecords = new byte[dataRecordCount][DATA_LENGTH];
            
            // データレコードをコピー
            for (int i = 0; i < dataRecordCount; i++) {
                int offset = HEADER_LENGTH + (i * DATA_LENGTH);
                System.arraycopy(data, offset, message.dataRecords[i], 0, DATA_LENGTH);
            }
        }
        
        // トレーラレコードを設定
        message.trailerRecord = new byte[TRAILER_LENGTH];
        System.arraycopy(data, data.length - TRAILER_LENGTH, message.trailerRecord, 0, TRAILER_LENGTH);
        
        return message;
    }
    
    /**
     * ヘッダレコードを作成します
     */
    private void createHeaderRecord() {
        // ヘッダレコード用のバッファを作成
        byte[] buffer = new byte[HEADER_LENGTH];
        
        // 固定値「ZEDI」を設定
        byte[] zedi = "ZEDI".getBytes(ZENGIN_CHARSET);
        System.arraycopy(zedi, 0, buffer, 0, zedi.length);
        
        // 送信者IDを設定（10バイト）
        byte[] senderIdBytes = padRight(senderId, 10).getBytes(ZENGIN_CHARSET);
        System.arraycopy(senderIdBytes, 0, buffer, 4, 10);
        
        // 受信者IDを設定（10バイト）
        byte[] receiverIdBytes = padRight(receiverId, 10).getBytes(ZENGIN_CHARSET);
        System.arraycopy(receiverIdBytes, 0, buffer, 14, 10);
        
        // 電文種別を設定（2バイト）
        byte[] messageTypeBytes = messageType.getCode().getBytes(ZENGIN_CHARSET);
        System.arraycopy(messageTypeBytes, 0, buffer, 24, 2);
        
        // 送信日時を設定（14バイト: YYYYMMDDHHmmss）
        String dateTimeStr = transmissionDateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        byte[] dateTimeBytes = dateTimeStr.getBytes(ZENGIN_CHARSET);
        System.arraycopy(dateTimeBytes, 0, buffer, 26, 14);
        
        // ファイルIDを設定（10バイト）
        byte[] fileIdBytes = padRight(fileId, 10).getBytes(ZENGIN_CHARSET);
        System.arraycopy(fileIdBytes, 0, buffer, 40, 10);
        
        // 残りは予備領域としてスペースで埋める
        for (int i = 50; i < HEADER_LENGTH; i++) {
            buffer[i] = 0x20; // スペース
        }
        
        this.headerRecord = buffer;
    }
    
    /**
     * トレーラレコードを作成します
     */
    private void createTrailerRecord() {
        // トレーラレコード用のバッファを作成
        byte[] buffer = new byte[TRAILER_LENGTH];
        
        // 固定値「ZEDI」を設定
        byte[] zedi = "ZEDI".getBytes(ZENGIN_CHARSET);
        System.arraycopy(zedi, 0, buffer, 0, zedi.length);
        
        // レコード種別「99」（トレーラ）を設定
        byte[] recordType = "99".getBytes(ZENGIN_CHARSET);
        System.arraycopy(recordType, 0, buffer, 4, 2);
        
        // データレコード件数を設定（8バイト）
        int recordCount = (dataRecords != null) ? dataRecords.length : 0;
        String countStr = String.format("%08d", recordCount);
        byte[] countBytes = countStr.getBytes(ZENGIN_CHARSET);
        System.arraycopy(countBytes, 0, buffer, 6, 8);
        
        // 残りは予備領域としてスペースで埋める
        for (int i = 14; i < TRAILER_LENGTH; i++) {
            buffer[i] = 0x20; // スペース
        }
        
        this.trailerRecord = buffer;
    }
    
    /**
     * ファイルIDを生成します
     * 
     * @return 生成されたファイルID
     */
    private String generateFileId() {
        // 現在時刻をもとにしたユニークなID
        return "F" + System.currentTimeMillis() % 1000000000;
    }
    
    /**
     * 文字列を指定長に右詰めします（不足分はスペース）
     * 
     * @param str 対象文字列
     * @param length 指定長
     * @return 右詰めされた文字列
     */
    private String padRight(String str, int length) {
        if (str == null) {
            str = "";
        }
        if (str.length() > length) {
            return str.substring(0, length);
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }
    
    /**
     * データレコードを設定します
     * 
     * @param dataRecords データレコード配列
     */
    public void setDataRecords(byte[][] dataRecords) {
        this.dataRecords = dataRecords;
    }
    
    /**
     * データレコードを取得します
     * 
     * @return データレコード配列
     */
    public byte[][] getDataRecords() {
        return dataRecords;
    }
    
    /**
     * 送信者IDを取得します
     * 
     * @return 送信者ID
     */
    public String getSenderId() {
        return senderId;
    }
    
    /**
     * 受信者IDを取得します
     * 
     * @return 受信者ID
     */
    public String getReceiverId() {
        return receiverId;
    }
    
    /**
     * 電文種別を取得します
     * 
     * @return 電文種別
     */
    public MessageType getMessageType() {
        return messageType;
    }
    
    /**
     * ファイルIDを取得します
     * 
     * @return ファイルID
     */
    public String getFileId() {
        return fileId;
    }
    
    /**
     * 送信日時を取得します
     * 
     * @return 送信日時
     */
    public LocalDateTime getTransmissionDateTime() {
        return transmissionDateTime;
    }
} 