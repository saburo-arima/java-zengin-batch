package com.example.zengin.security;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.example.zengin.communication.ZenginCommunicationException;
import com.example.zengin.config.TestTlsConfig;
import com.example.zengin.format.ZenginMessage;
import com.example.zengin.format.ZenginMessage.MessageType;

/**
 * メッセージ整合性チェック機能のテストクラス
 */
@SpringBootTest
@Import(TestTlsConfig.class)
public class MessageIntegrityTest {
    
    @Autowired
    private MessageIntegrityUtil integrityUtil;
    
    @Autowired
    private MessageIntegrityService integrityService;
    
    @Autowired
    private MessageIntegrityRepository integrityRepository;
    
    private ZenginMessage testMessage;
    private byte[][] testDataRecords;
    
    @BeforeEach
    public void setUp() {
        // テスト用データレコードを作成
        testDataRecords = new byte[3][120];
        
        // データレコード1: 金額10000円
        byte[] record1 = "RECORD1DATA".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(record1, 0, testDataRecords[0], 0, record1.length);
        byte[] amount1 = "000000010000".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(amount1, 0, testDataRecords[0], 30, amount1.length);
        
        // データレコード2: 金額20000円
        byte[] record2 = "RECORD2DATA".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(record2, 0, testDataRecords[1], 0, record2.length);
        byte[] amount2 = "000000020000".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(amount2, 0, testDataRecords[1], 30, amount2.length);
        
        // データレコード3: 金額30000円
        byte[] record3 = "RECORD3DATA".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(record3, 0, testDataRecords[2], 0, record3.length);
        byte[] amount3 = "000000030000".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(amount3, 0, testDataRecords[2], 30, amount3.length);
        
        // テスト用メッセージを作成
        testMessage = new ZenginMessage(MessageType.TRANSFER, "TESTSENDER", "TESTRECEIVER");
        testMessage.setDataRecords(testDataRecords);
    }
    
    @Test
    public void testCalculateHash() throws NoSuchAlgorithmException {
        // メッセージのバイト配列を取得
        byte[] messageData = testMessage.toByteArray();
        
        // ハッシュ値を計算
        String hashValue = integrityUtil.calculateHash(messageData);
        
        // ハッシュ値が空でないことを確認
        assertNotNull(hashValue);
        assertTrue(hashValue.length() > 0);
        
        // 同じデータで再計算した場合、同じハッシュ値になることを確認
        String hashValue2 = integrityUtil.calculateHash(messageData);
        assertEquals(hashValue, hashValue2);
        
        // データを変更した場合、異なるハッシュ値になることを確認
        messageData[10] = (byte) (messageData[10] + 1);
        String hashValue3 = integrityUtil.calculateHash(messageData);
        assertNotEquals(hashValue, hashValue3);
    }
    
    @Test
    public void testVerifyHash() throws NoSuchAlgorithmException {
        // メッセージのバイト配列を取得
        byte[] messageData = testMessage.toByteArray();
        
        // ハッシュ値を計算
        String hashValue = integrityUtil.calculateHash(messageData);
        
        // 正しいハッシュ値で検証
        boolean result = integrityUtil.verifyHash(messageData, hashValue);
        assertTrue(result);
        
        // 不正なハッシュ値で検証
        boolean result2 = integrityUtil.verifyHash(messageData, hashValue + "X");
        assertFalse(result2);
    }
    
    @Test
    public void testCalculateRecordCount() {
        // レコード件数を計算
        int recordCount = integrityUtil.calculateRecordCount(testDataRecords);
        
        // 期待値と一致することを確認
        assertEquals(3, recordCount);
    }
    
    @Test
    public void testCalculateTotalAmount() {
        // 合計金額を計算
        long totalAmount = integrityUtil.calculateTotalAmount(testDataRecords, 30, 12);
        
        // 期待値と一致することを確認（10000 + 20000 + 30000 = 60000）
        assertEquals(60000, totalAmount);
    }
    
    @Test
    public void testGenerateAndSaveIntegrityInfo() throws ZenginCommunicationException {
        // 整合性情報を生成・保存
        MessageIntegrityInfo integrityInfo = integrityService.generateAndSaveIntegrityInfo(testMessage);
        
        // 生成された整合性情報を確認
        assertNotNull(integrityInfo);
        assertEquals(testMessage.getFileId(), integrityInfo.getMessageId());
        assertEquals(3, integrityInfo.getRecordCount());
        assertEquals(60000, integrityInfo.getTotalAmount());
        assertNotNull(integrityInfo.getHashValue());
        
        // リポジトリから取得して一致することを確認
        MessageIntegrityInfo savedInfo = integrityRepository.findByMessageId(testMessage.getFileId()).orElse(null);
        assertNotNull(savedInfo);
        assertEquals(integrityInfo.getHashValue(), savedInfo.getHashValue());
    }
    
    @Test
    public void testVerifyMessageIntegrity() throws ZenginCommunicationException {
        // 整合性情報を生成・保存
        integrityService.generateAndSaveIntegrityInfo(testMessage);
        
        // 同じメッセージで整合性を検証
        boolean result = integrityService.verifyMessageIntegrity(testMessage);
        assertTrue(result);
        
        // メッセージを変更した場合、検証に失敗することを確認
        ZenginMessage modifiedMessage = new ZenginMessage(MessageType.TRANSFER, "TESTSENDER", "TESTRECEIVER");
        modifiedMessage.setDataRecords(testDataRecords);
        
        // ファイルIDを同じにする（検証対象を同じにするため）
        try {
            java.lang.reflect.Field fileIdField = ZenginMessage.class.getDeclaredField("fileId");
            fileIdField.setAccessible(true);
            fileIdField.set(modifiedMessage, testMessage.getFileId());
        } catch (Exception e) {
            fail("テスト準備中にエラーが発生しました: " + e.getMessage());
        }
        
        // データレコードを変更
        byte[][] modifiedDataRecords = new byte[3][120];
        System.arraycopy(testDataRecords, 0, modifiedDataRecords, 0, testDataRecords.length);
        modifiedDataRecords[0][5] = (byte) (modifiedDataRecords[0][5] + 1);
        modifiedMessage.setDataRecords(modifiedDataRecords);
        
        // 変更したメッセージで検証
        boolean result2 = integrityService.verifyMessageIntegrity(modifiedMessage);
        assertFalse(result2);
    }
    
    @Test
    public void testIsDuplicateMessage() throws ZenginCommunicationException {
        // 整合性情報を生成・保存
        integrityService.generateAndSaveIntegrityInfo(testMessage);
        
        // 同じメッセージIDで重複チェック
        boolean isDuplicate = integrityService.isDuplicateMessage(testMessage.getFileId());
        assertTrue(isDuplicate);
        
        // 異なるメッセージIDで重複チェック
        boolean isDuplicate2 = integrityService.isDuplicateMessage("DIFFERENT_ID");
        assertFalse(isDuplicate2);
    }
} 