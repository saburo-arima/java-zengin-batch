package com.example.zengin.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.zengin.communication.ZenginCommunicationException;
import com.example.zengin.communication.ZenginTcpIpProtocol;
import com.example.zengin.format.ZenginMessage;
import com.example.zengin.format.ZenginMessage.MessageType;
import com.example.zengin.security.MessageIntegrityService;

import java.nio.charset.StandardCharsets;

/**
 * 全銀通信サービスのテストクラス
 */
@ExtendWith(MockitoExtension.class)
public class ZenginCommunicationServiceTest {

    @InjectMocks
    private ZenginCommunicationService communicationService;

    @Mock
    private ZenginTcpIpProtocol zenginProtocol;

    @Mock
    private MessageIntegrityService integrityService;

    private static final String TEST_SENDER_ID = "TESTSENDER";
    private static final String TEST_RECEIVER_ID = "TESTRECEIVER";
    private static final String TEST_HOST = "testhost.example.com";
    private static final int TEST_PORT = 20000;

    private ZenginMessage testMessage;
    private byte[][] testDataRecords;

    @BeforeEach
    public void setUp() {
        // テスト用データレコードを作成
        testDataRecords = new byte[2][120];
        
        // データレコード1
        byte[] record1 = "RECORD1DATA".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(record1, 0, testDataRecords[0], 0, record1.length);
        
        // データレコード2
        byte[] record2 = "RECORD2DATA".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(record2, 0, testDataRecords[1], 0, record2.length);
        
        // テスト用メッセージを作成
        testMessage = new ZenginMessage(MessageType.TRANSFER, TEST_SENDER_ID, TEST_RECEIVER_ID);
        testMessage.setDataRecords(testDataRecords);
        
        // サービスの設定
        ReflectionTestUtils.setField(communicationService, "bankHost", TEST_HOST);
        ReflectionTestUtils.setField(communicationService, "bankPort", TEST_PORT);
        ReflectionTestUtils.setField(communicationService, "senderId", TEST_SENDER_ID);
        ReflectionTestUtils.setField(communicationService, "integrityCheckEnabled", true);
    }

    @Test
    public void testSendTransferData_Success() throws Exception {
        // モックの設定
        when(zenginProtocol.sendData(eq(TEST_HOST), eq(TEST_PORT), any(byte[].class))).thenReturn(true);
        
        // 振込データ送信実行
        boolean result = communicationService.sendTransferData(TEST_RECEIVER_ID, testDataRecords);
        
        // 結果確認
        assertTrue(result);
        
        // 整合性チェックが実行されたことを確認
        verify(integrityService).generateAndSaveIntegrityInfo(any(ZenginMessage.class));
        
        // プロトコルのsendDataが呼ばれたことを確認
        verify(zenginProtocol).sendData(eq(TEST_HOST), eq(TEST_PORT), any(byte[].class));
    }

    @Test
    public void testSendTransferData_WithIntegrityCheckDisabled() throws Exception {
        // 整合性チェック無効設定
        ReflectionTestUtils.setField(communicationService, "integrityCheckEnabled", false);
        
        // モックの設定
        when(zenginProtocol.sendData(eq(TEST_HOST), eq(TEST_PORT), any(byte[].class))).thenReturn(true);
        
        // 振込データ送信実行
        boolean result = communicationService.sendTransferData(TEST_RECEIVER_ID, testDataRecords);
        
        // 結果確認
        assertTrue(result);
        
        // 整合性チェックが実行されないことを確認
        verify(integrityService, never()).generateAndSaveIntegrityInfo(any(ZenginMessage.class));
        
        // プロトコルのsendDataが呼ばれたことを確認
        verify(zenginProtocol).sendData(eq(TEST_HOST), eq(TEST_PORT), any(byte[].class));
    }

    @Test
    public void testSendTransferData_Failure() throws Exception {
        // モックの設定
        when(zenginProtocol.sendData(eq(TEST_HOST), eq(TEST_PORT), any(byte[].class))).thenReturn(false);
        
        // 振込データ送信実行
        boolean result = communicationService.sendTransferData(TEST_RECEIVER_ID, testDataRecords);
        
        // 結果確認
        assertFalse(result);
        
        // 整合性チェックが実行されたことを確認
        verify(integrityService).generateAndSaveIntegrityInfo(any(ZenginMessage.class));
    }

    @Test
    public void testSendTransferData_Exception() throws Exception {
        // モックの設定
        when(zenginProtocol.sendData(eq(TEST_HOST), eq(TEST_PORT), any(byte[].class)))
            .thenThrow(new ZenginCommunicationException("テスト例外", "E999"));
        
        // 振込データ送信実行で例外が発生することを確認
        assertThrows(ZenginCommunicationException.class, () -> {
            communicationService.sendTransferData(TEST_RECEIVER_ID, testDataRecords);
        });
    }

    @Test
    public void testSendInquiryAndReceiveResponse_Success() throws Exception {
        // 送信モックの設定
        when(zenginProtocol.sendData(eq(TEST_HOST), eq(TEST_PORT), any(byte[].class))).thenReturn(true);
        
        // 応答メッセージの準備
        ZenginMessage responseMessage = new ZenginMessage(MessageType.RESPONSE, TEST_RECEIVER_ID, TEST_SENDER_ID);
        responseMessage.setDataRecords(testDataRecords);
        byte[] responseBytes = responseMessage.toByteArray();
        
        // 受信モックの設定
        when(zenginProtocol.receiveData(eq(TEST_HOST), eq(TEST_PORT))).thenReturn(responseBytes);
        
        // 重複チェックモックの設定
        when(integrityService.isDuplicateMessage(anyString())).thenReturn(false);
        
        // 照会送信・応答受信実行
        ZenginMessage result = communicationService.sendInquiryAndReceiveResponse(TEST_RECEIVER_ID, testDataRecords);
        
        // 結果確認
        assertNotNull(result);
        assertEquals(MessageType.RESPONSE, result.getMessageType());
        assertEquals(TEST_RECEIVER_ID, result.getSenderId());
        assertEquals(TEST_SENDER_ID, result.getReceiverId());
        
        // 整合性チェックが実行されたことを確認
        verify(integrityService).generateAndSaveIntegrityInfo(any(ZenginMessage.class));
        verify(integrityService).isDuplicateMessage(anyString());
        
        // プロトコルのsendDataとreceiveDataが呼ばれたことを確認
        verify(zenginProtocol).sendData(eq(TEST_HOST), eq(TEST_PORT), any(byte[].class));
        verify(zenginProtocol).receiveData(eq(TEST_HOST), eq(TEST_PORT));
    }

    @Test
    public void testSendInquiryAndReceiveResponse_InvalidResponseType() throws Exception {
        // 送信モックの設定
        when(zenginProtocol.sendData(eq(TEST_HOST), eq(TEST_PORT), any(byte[].class))).thenReturn(true);
        
        // 不正な応答種別のメッセージを準備
        ZenginMessage invalidResponseMessage = new ZenginMessage(MessageType.NOTIFICATION, TEST_RECEIVER_ID, TEST_SENDER_ID);
        invalidResponseMessage.setDataRecords(testDataRecords);
        byte[] responseBytes = invalidResponseMessage.toByteArray();
        
        // 受信モックの設定
        when(zenginProtocol.receiveData(eq(TEST_HOST), eq(TEST_PORT))).thenReturn(responseBytes);
        
        // 照会送信・応答受信実行で例外が発生することを確認
        assertThrows(ZenginCommunicationException.class, () -> {
            communicationService.sendInquiryAndReceiveResponse(TEST_RECEIVER_ID, testDataRecords);
        });
    }

    @Test
    public void testSendInquiryAndReceiveResponse_DuplicateMessage() throws Exception {
        // 送信モックの設定
        when(zenginProtocol.sendData(eq(TEST_HOST), eq(TEST_PORT), any(byte[].class))).thenReturn(true);
        
        // 応答メッセージの準備
        ZenginMessage responseMessage = new ZenginMessage(MessageType.RESPONSE, TEST_RECEIVER_ID, TEST_SENDER_ID);
        responseMessage.setDataRecords(testDataRecords);
        byte[] responseBytes = responseMessage.toByteArray();
        
        // 受信モックの設定
        when(zenginProtocol.receiveData(eq(TEST_HOST), eq(TEST_PORT))).thenReturn(responseBytes);
        
        // 重複チェックモックの設定（重複あり）
        when(integrityService.isDuplicateMessage(anyString())).thenReturn(true);
        
        // 照会送信・応答受信実行で例外が発生することを確認
        assertThrows(ZenginCommunicationException.class, () -> {
            communicationService.sendInquiryAndReceiveResponse(TEST_RECEIVER_ID, testDataRecords);
        });
    }

    @Test
    public void testReceiveNotification_Success() throws Exception {
        // 通知メッセージの準備
        ZenginMessage notificationMessage = new ZenginMessage(MessageType.NOTIFICATION, TEST_RECEIVER_ID, TEST_SENDER_ID);
        notificationMessage.setDataRecords(testDataRecords);
        byte[] notificationBytes = notificationMessage.toByteArray();
        
        // 受信モックの設定
        when(zenginProtocol.receiveData(eq(TEST_HOST), eq(TEST_PORT))).thenReturn(notificationBytes);
        
        // 重複チェックモックの設定
        when(integrityService.isDuplicateMessage(anyString())).thenReturn(false);
        
        // 通知受信実行
        ZenginMessage result = communicationService.receiveNotification();
        
        // 結果確認
        assertNotNull(result);
        assertEquals(MessageType.NOTIFICATION, result.getMessageType());
        assertEquals(TEST_RECEIVER_ID, result.getSenderId());
        assertEquals(TEST_SENDER_ID, result.getReceiverId());
        
        // 整合性チェックが実行されたことを確認
        verify(integrityService).generateAndSaveIntegrityInfo(any(ZenginMessage.class));
        verify(integrityService).isDuplicateMessage(anyString());
        
        // プロトコルのreceiveDataが呼ばれたことを確認
        verify(zenginProtocol).receiveData(eq(TEST_HOST), eq(TEST_PORT));
    }

    @Test
    public void testReceiveNotification_InvalidMessageType() throws Exception {
        // 不正な通知種別のメッセージを準備
        ZenginMessage invalidMessage = new ZenginMessage(MessageType.TRANSFER, TEST_RECEIVER_ID, TEST_SENDER_ID);
        invalidMessage.setDataRecords(testDataRecords);
        byte[] invalidBytes = invalidMessage.toByteArray();
        
        // 受信モックの設定
        when(zenginProtocol.receiveData(eq(TEST_HOST), eq(TEST_PORT))).thenReturn(invalidBytes);
        
        // 通知受信実行で例外が発生することを確認
        assertThrows(ZenginCommunicationException.class, () -> {
            communicationService.receiveNotification();
        });
    }

    @Test
    public void testGetterAndSetter() {
        // 初期値確認
        assertEquals(TEST_HOST, communicationService.getBankHost());
        assertEquals(TEST_PORT, communicationService.getBankPort());
        assertTrue(communicationService.isIntegrityCheckEnabled());
        
        // 値を変更
        communicationService.setBankHost("newhost.example.com");
        communicationService.setBankPort(30000);
        communicationService.setIntegrityCheckEnabled(false);
        
        // 変更後の値を確認
        assertEquals("newhost.example.com", communicationService.getBankHost());
        assertEquals(30000, communicationService.getBankPort());
        assertFalse(communicationService.isIntegrityCheckEnabled());
    }
} 