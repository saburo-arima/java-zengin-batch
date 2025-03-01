package com.example.zengin.communication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 全銀TCP/IPプロトコル実装のテストクラス
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SpringBootTest
public class ZenginTcpIpProtocolTest {

    @Spy
    private ZenginTcpIpProtocolImpl zenginProtocol;

    @Mock
    private Socket socket;

    @Mock
    private SSLSocketFactory sslSocketFactory;

    @Mock
    private SSLSocket sslSocket;

    private ByteArrayOutputStream outputStream;
    private ByteArrayInputStream inputStream;

    private static final byte STX = 0x02;
    private static final byte ETX = 0x03;
    private static final byte EOT = 0x04;
    private static final byte ENQ = 0x05;
    private static final byte ACK = 0x06;
    private static final byte NAK = 0x15;

    @BeforeEach
    public void setUp() throws Exception {
        // 通常のMockitoAnnotationsを使用
        MockitoAnnotations.openMocks(this);
        
        // 出力ストリームをモック
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void testConnect_Success() throws Exception {
        // 出力ストリームの設定
        when(sslSocket.getOutputStream()).thenReturn(outputStream);
        
        // 入力ストリームの準備（ENQに対してACKを返す）
        byte[] responseData = new byte[] { ACK };
        inputStream = new ByteArrayInputStream(responseData);
        when(sslSocket.getInputStream()).thenReturn(inputStream);
        
        // SSLSocketFactoryを使用するようにモック設定
        ReflectionTestUtils.setField(zenginProtocol, "useTLS", true);
        
        // SSLSocketFactoryを直接設定
        doReturn(sslSocketFactory).when(zenginProtocol).getSSLSocketFactory();
        when(sslSocketFactory.createSocket(anyString(), anyInt())).thenReturn(sslSocket);
        
        // 強力な暗号スイートの設定
        when(sslSocket.getSupportedCipherSuites()).thenReturn(new String[] {
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
        });
        
        // 接続実行
        zenginProtocol.connect("localhost", 20000);
        
        // ENQが送信されたことを確認
        assertEquals(ENQ, outputStream.toByteArray()[0]);
        
        // ソケットとストリームが設定されていることを確認
        assertNotNull(ReflectionTestUtils.getField(zenginProtocol, "socket"));
        assertNotNull(ReflectionTestUtils.getField(zenginProtocol, "inputStream"));
        assertNotNull(ReflectionTestUtils.getField(zenginProtocol, "outputStream"));
    }

    @Test
    public void testConnect_InvalidResponse() throws Exception {
        // 出力ストリームの設定
        when(sslSocket.getOutputStream()).thenReturn(outputStream);
        
        // 入力ストリームの準備（ENQに対してNAKを返す）
        byte[] responseData = new byte[] { NAK };
        inputStream = new ByteArrayInputStream(responseData);
        when(sslSocket.getInputStream()).thenReturn(inputStream);
        
        // SSLSocketFactoryを使用するようにモック設定
        ReflectionTestUtils.setField(zenginProtocol, "useTLS", true);
        
        // SSLSocketFactoryを直接設定
        doReturn(sslSocketFactory).when(zenginProtocol).getSSLSocketFactory();
        when(sslSocketFactory.createSocket(anyString(), anyInt())).thenReturn(sslSocket);
        
        // 強力な暗号スイートの設定
        when(sslSocket.getSupportedCipherSuites()).thenReturn(new String[] {
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
        });
        
        // 接続実行で例外が発生することを確認
        assertThrows(ZenginCommunicationException.class, () -> {
            zenginProtocol.connect("localhost", 20000);
        });
    }

    @Test
    public void testDisconnect_Success() throws Exception {
        // 出力ストリームの設定
        when(socket.getOutputStream()).thenReturn(outputStream);
        
        // 接続状態を設定
        ReflectionTestUtils.setField(zenginProtocol, "socket", socket);
        ReflectionTestUtils.setField(zenginProtocol, "inputStream", inputStream);
        ReflectionTestUtils.setField(zenginProtocol, "outputStream", outputStream);
        
        // 切断実行
        zenginProtocol.disconnect();
        
        // EOTが送信されたことを確認
        assertEquals(EOT, outputStream.toByteArray()[0]);
        
        // ソケットとストリームがクローズされたことを確認
        verify(socket).close();
        
        // フィールドがリセットされていることを確認
        assertNull(ReflectionTestUtils.getField(zenginProtocol, "socket"));
        assertNull(ReflectionTestUtils.getField(zenginProtocol, "inputStream"));
        assertNull(ReflectionTestUtils.getField(zenginProtocol, "outputStream"));
    }

    @Test
    public void testSendData_Success() throws Exception {
        // 出力ストリームの設定
        when(socket.getOutputStream()).thenReturn(outputStream);
        
        // 入力ストリームの準備（データ送信後にACKを返す）
        byte[] responseData = new byte[] { ACK };
        inputStream = new ByteArrayInputStream(responseData);
        when(socket.getInputStream()).thenReturn(inputStream);
        
        // モックの設定
        ReflectionTestUtils.setField(zenginProtocol, "socket", socket);
        ReflectionTestUtils.setField(zenginProtocol, "inputStream", inputStream);
        ReflectionTestUtils.setField(zenginProtocol, "outputStream", outputStream);
        
        // テストデータ
        byte[] testData = "TEST_DATA".getBytes();
        
        // データ送信実行
        boolean result = zenginProtocol.sendData("localhost", 20000, testData);
        
        // 送信成功を確認
        assertTrue(result);
        
        // 送信データを確認（STX + データ + ETX）
        byte[] sentData = outputStream.toByteArray();
        assertEquals(STX, sentData[0]);
        assertEquals('T', sentData[1]); // TESTの先頭文字
        assertEquals(ETX, sentData[sentData.length - 1]);
    }

    @Test
    public void testReceiveData_Success() throws Exception {
        // 出力ストリームの設定
        when(socket.getOutputStream()).thenReturn(outputStream);
        
        // テストデータ（STX + データ + ETX）
        byte[] testData = new byte[12];
        testData[0] = STX;
        System.arraycopy("TEST_DATA".getBytes(), 0, testData, 1, 9);
        testData[10] = ETX;
        testData[11] = ACK; // 応答確認用
        
        // 入力ストリームの準備
        inputStream = new ByteArrayInputStream(testData);
        when(socket.getInputStream()).thenReturn(inputStream);
        
        // モックの設定
        ReflectionTestUtils.setField(zenginProtocol, "socket", socket);
        ReflectionTestUtils.setField(zenginProtocol, "inputStream", inputStream);
        ReflectionTestUtils.setField(zenginProtocol, "outputStream", outputStream);
        
        // データ受信実行
        byte[] receivedData = zenginProtocol.receiveData("localhost", 20000);
        
        // 受信データを確認
        assertNotNull(receivedData);
        assertEquals(9, receivedData.length);
        assertEquals('T', receivedData[0]); // TESTの先頭文字
        
        // ACKが送信されたことを確認
        assertEquals(ACK, outputStream.toByteArray()[0]);
    }

    @Test
    public void testNonTlsConnection() throws Exception {
        // 出力ストリームの設定
        when(socket.getOutputStream()).thenReturn(outputStream);
        
        // TLS無効設定
        ReflectionTestUtils.setField(zenginProtocol, "useTLS", false);
        
        // 入力ストリームの準備（ENQに対してACKを返す）
        byte[] responseData = new byte[] { ACK };
        inputStream = new ByteArrayInputStream(responseData);
        when(socket.getInputStream()).thenReturn(inputStream);
        
        // 通常ソケットのモック
        doReturn(socket).when(zenginProtocol).createNonTLSSocket(anyString(), anyInt());
        
        // 接続実行
        zenginProtocol.connect("localhost", 20000);
        
        // 通常ソケットが使用されたことを確認
        verify(zenginProtocol).createNonTLSSocket(eq("localhost"), eq(20000));
        
        // ENQが送信されたことを確認
        assertEquals(ENQ, outputStream.toByteArray()[0]);
    }
}