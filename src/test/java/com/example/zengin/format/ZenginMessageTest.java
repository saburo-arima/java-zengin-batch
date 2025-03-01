package com.example.zengin.format;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.zengin.format.ZenginMessage.MessageType;

/**
 * 全銀メッセージクラスのテスト
 */
public class ZenginMessageTest {

    private static final String TEST_SENDER_ID = "TESTSENDER";
    private static final String TEST_RECEIVER_ID = "TESTRECEIVER";
    private static final String TEST_FILE_ID = "TEST00001";
    
    private ZenginMessage testMessage;
    private byte[][] testDataRecords;
    
    @BeforeEach
    public void setUp() {
        // テスト用データレコードを作成
        testDataRecords = new byte[2][120]; // DATA_LENGTH = 120
        
        // データレコード1
        byte[] record1 = "RECORD1DATA".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(record1, 0, testDataRecords[0], 0, record1.length);
        
        // データレコード2
        byte[] record2 = "RECORD2DATA".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(record2, 0, testDataRecords[1], 0, record2.length);
        
        // テスト用メッセージを作成
        testMessage = new ZenginMessage(MessageType.TRANSFER, TEST_SENDER_ID, TEST_RECEIVER_ID);
        testMessage.setDataRecords(testDataRecords);
        
        // リフレクションを使用してファイルIDを設定
        try {
            java.lang.reflect.Field fileIdField = ZenginMessage.class.getDeclaredField("fileId");
            fileIdField.setAccessible(true);
            fileIdField.set(testMessage, TEST_FILE_ID);
        } catch (Exception e) {
            fail("テスト準備中にエラーが発生しました: " + e.getMessage());
        }
    }
    
    @Test
    public void testConstructor() {
        // コンストラクタで設定された値を確認
        assertEquals(MessageType.TRANSFER, testMessage.getMessageType());
        assertEquals(TEST_SENDER_ID, testMessage.getSenderId());
        assertEquals(TEST_RECEIVER_ID, testMessage.getReceiverId());
        
        // 現在日時が設定されていることを確認
        assertNotNull(testMessage.getTransmissionDateTime());
        
        // ファイルIDが設定されていることを確認
        assertNotNull(getFileId(testMessage));
    }
    
    @Test
    public void testSetAndGetDataRecords() {
        // データレコードが正しく設定されていることを確認
        assertArrayEquals(testDataRecords, testMessage.getDataRecords());
        
        // 新しいデータレコードを設定
        byte[][] newDataRecords = new byte[1][120]; // DATA_LENGTH = 120
        byte[] newRecord = "NEWRECORD".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(newRecord, 0, newDataRecords[0], 0, newRecord.length);
        
        testMessage.setDataRecords(newDataRecords);
        
        // 新しいデータレコードが正しく設定されていることを確認
        assertArrayEquals(newDataRecords, testMessage.getDataRecords());
    }
    
    @Test
    public void testToByteArray() {
        // メッセージをバイト配列に変換
        byte[] messageBytes = testMessage.toByteArray();
        
        // バイト配列の長さを確認
        int expectedLength = 80 + // HEADER_LENGTH
                             (testDataRecords.length * 120) + // DATA_LENGTH
                             80; // TRAILER_LENGTH
        assertEquals(expectedLength, messageBytes.length);
        
        // ヘッダーレコードに送信者IDが含まれていることを確認
        byte[] headerBytes = new byte[80]; // HEADER_LENGTH
        System.arraycopy(messageBytes, 0, headerBytes, 0, 80);
        String headerStr = new String(headerBytes, StandardCharsets.UTF_8);
        assertTrue(headerStr.contains(TEST_SENDER_ID));
        
        // データレコードが含まれていることを確認
        byte[] firstDataRecord = new byte[120]; // DATA_LENGTH
        System.arraycopy(messageBytes, 80, firstDataRecord, 0, 120);
        String firstDataStr = new String(firstDataRecord, StandardCharsets.UTF_8);
        assertTrue(firstDataStr.contains("RECORD1DATA"));
        
        // トレーラーレコードが含まれていることを確認
        byte[] trailerBytes = new byte[80]; // TRAILER_LENGTH
        System.arraycopy(messageBytes, messageBytes.length - 80, trailerBytes, 0, 80);
        String trailerStr = new String(trailerBytes, StandardCharsets.UTF_8);
        assertTrue(trailerStr.contains("00000002")); // レコード件数
    }
    
    @Test
    public void testFromByteArray() {
        // メッセージをバイト配列に変換
        byte[] messageBytes = testMessage.toByteArray();
        
        // バイト配列からメッセージを復元
        ZenginMessage restoredMessage = ZenginMessage.fromByteArray(messageBytes);
        
        // 復元されたメッセージの値を確認
        assertEquals(testMessage.getMessageType(), restoredMessage.getMessageType());
        assertEquals(testMessage.getSenderId(), restoredMessage.getSenderId());
        assertEquals(testMessage.getReceiverId(), restoredMessage.getReceiverId());
        assertEquals(getFileId(testMessage), getFileId(restoredMessage));
        
        // データレコード数を確認
        assertEquals(testMessage.getDataRecords().length, restoredMessage.getDataRecords().length);
        
        // 最初のデータレコードの内容を確認
        String originalData = new String(testMessage.getDataRecords()[0], StandardCharsets.UTF_8);
        String restoredData = new String(restoredMessage.getDataRecords()[0], StandardCharsets.UTF_8);
        assertTrue(restoredData.contains("RECORD1DATA"));
    }
    
    @Test
    public void testCreateHeaderRecord() {
        // ヘッダーレコードを作成するためにtoByteArrayを呼び出す
        byte[] messageBytes = testMessage.toByteArray();
        
        // ヘッダーレコードを取得
        byte[] headerBytes = new byte[80]; // HEADER_LENGTH
        System.arraycopy(messageBytes, 0, headerBytes, 0, 80);
        
        // ヘッダーレコードの内容を確認
        String headerStr = new String(headerBytes, StandardCharsets.UTF_8);
        assertTrue(headerStr.contains(TEST_SENDER_ID));
        assertTrue(headerStr.contains(TEST_RECEIVER_ID));
        assertTrue(headerStr.contains(TEST_FILE_ID));
        
        // 現在日時のフォーマットを確認
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String currentDateTime = LocalDateTime.now().format(formatter);
        String dateTimePart = currentDateTime.substring(0, 8); // 日付部分のみ
        assertTrue(headerStr.contains(dateTimePart));
    }
    
    @Test
    public void testCreateTrailerRecord() {
        // トレーラーレコードを作成するためにtoByteArrayを呼び出す
        byte[] messageBytes = testMessage.toByteArray();
        
        // トレーラーレコードを取得
        byte[] trailerBytes = new byte[80]; // TRAILER_LENGTH
        System.arraycopy(messageBytes, messageBytes.length - 80, trailerBytes, 0, 80);
        
        // トレーラーレコードの内容を確認
        String trailerStr = new String(trailerBytes, StandardCharsets.UTF_8);
        assertTrue(trailerStr.contains("00000002")); // レコード件数
    }
    
    @Test
    public void testMessageTypeEnum() {
        // 各メッセージタイプのコード値を確認
        assertEquals("01", MessageType.TRANSFER.getCode());
        assertEquals("02", MessageType.INQUIRY.getCode());
        assertEquals("03", MessageType.RESPONSE.getCode());
        assertEquals("04", MessageType.NOTIFICATION.getCode());
        
        // コードからメッセージタイプを取得
        assertEquals(MessageType.TRANSFER, findMessageTypeByCode("01"));
        assertEquals(MessageType.INQUIRY, findMessageTypeByCode("02"));
        assertEquals(MessageType.RESPONSE, findMessageTypeByCode("03"));
        assertEquals(MessageType.NOTIFICATION, findMessageTypeByCode("04"));
        
        // 不正なコードの場合はnullが返されることを確認
        assertNull(findMessageTypeByCode("99"));
    }
    
    /**
     * リフレクションを使用してファイルIDを取得するヘルパーメソッド
     */
    private String getFileId(ZenginMessage message) {
        try {
            java.lang.reflect.Field fileIdField = ZenginMessage.class.getDeclaredField("fileId");
            fileIdField.setAccessible(true);
            return (String) fileIdField.get(message);
        } catch (Exception e) {
            fail("ファイルID取得中にエラーが発生しました: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * コードからMessageTypeを検索するヘルパーメソッド
     */
    private MessageType findMessageTypeByCode(String code) {
        for (MessageType type : MessageType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
} 