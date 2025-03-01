package com.example.zengin.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.stereotype.Component;

/**
 * 全銀TCP/IPプロトコルの実装クラス
 * 全銀協標準通信プロトコル（TCP/IP手順）に準拠した送受信処理を実装します
 */
@Component
public class ZenginTcpIpProtocolImpl implements ZenginTcpIpProtocol {
    
    private static final Logger logger = Logger.getLogger(ZenginTcpIpProtocolImpl.class.getName());
    
    // 全銀プロトコル定数
    private static final byte STX = 0x02; // 通信開始文字
    private static final byte ETX = 0x03; // 通信終了文字
    private static final byte EOT = 0x04; // 転送終了文字
    private static final byte ENQ = 0x05; // 問い合わせ文字
    private static final byte ACK = 0x06; // 肯定応答文字
    private static final byte NAK = 0x15; // 否定応答文字
    private static final byte DLE = 0x10; // データリンクエスケープ
    
    // 通信制御用変数
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final AtomicInteger sequenceNumber = new AtomicInteger(0); // TTCシーケンス番号
    private boolean useTLS = true; // デフォルトでTLS使用
    
    /**
     * コンストラクタ
     */
    public ZenginTcpIpProtocolImpl() {
        // デフォルトコンストラクタ
    }
    
    @Override
    public void connect(String hostAddress, int port) throws ZenginCommunicationException {
        try {
            if (useTLS) {
                // SSL/TLS接続を確立
                SSLSocketFactory sslSocketFactory = getSSLSocketFactory();
                SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(hostAddress, port);
                
                // TLS 1.2以上を使用
                sslSocket.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
                
                // 強力な暗号スイートのみを使用
                String[] strongCipherSuites = getStrongCipherSuites(sslSocket.getSupportedCipherSuites());
                sslSocket.setEnabledCipherSuites(strongCipherSuites);
                
                sslSocket.startHandshake();
                this.socket = sslSocket;
            } else {
                // 非SSL接続（テスト用または閉域網用）
                this.socket = createNonTLSSocket(hostAddress, port);
            }
            
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
            
            // 接続要求（ENQ）を送信
            sendControlCharacter(ENQ);
            
            // ACK応答を待機
            byte response = readControlCharacter();
            if (response != ACK) {
                throw new ZenginCommunicationException(
                    "接続要求に対する応答が不正です: " + response, 
                    "E001"
                );
            }
            
            logger.info("全銀TCP/IP接続が確立されました: " + hostAddress + ":" + port);
            
        } catch (IOException e) {
            throw new ZenginCommunicationException("全銀TCP/IP接続に失敗しました: " + e.getMessage(), e, "E002");
        }
    }
    
    @Override
    public void disconnect() throws ZenginCommunicationException {
        if (socket == null || socket.isClosed()) {
            return;
        }
        
        try {
            // 終了シーケンス（EOT）を送信
            sendControlCharacter(EOT);
            
            // リソースをクローズ
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            socket.close();
            
            // 状態をリセット
            socket = null;
            inputStream = null;
            outputStream = null;
            
            logger.info("全銀TCP/IP接続を切断しました");
            
        } catch (IOException e) {
            throw new ZenginCommunicationException("全銀TCP/IP切断中にエラーが発生しました: " + e.getMessage(), e, "E003");
        }
    }
    
    @Override
    public boolean sendData(String hostAddress, int port, byte[] data) throws ZenginCommunicationException {
        if (socket == null || socket.isClosed()) {
            connect(hostAddress, port);
        }
        
        try {
            // データ送信シーケンス
            // 1. STXを送信
            outputStream.write(STX);
            
            // 2. データ本体を送信
            outputStream.write(data);
            
            // 3. ETXを送信
            outputStream.write(ETX);
            outputStream.flush();
            
            // 4. ACK応答を待機
            byte response = readControlCharacter();
            if (response != ACK) {
                throw new ZenginCommunicationException(
                    "データ送信に対する応答が不正です: " + response, 
                    "E004"
                );
            }
            
            logger.info("全銀データを送信しました: " + data.length + " バイト");
            return true;
            
        } catch (IOException e) {
            throw new ZenginCommunicationException("データ送信中にエラーが発生しました: " + e.getMessage(), e, "E005");
        }
    }
    
    @Override
    public byte[] receiveData(String hostAddress, int port) throws ZenginCommunicationException {
        if (socket == null || socket.isClosed()) {
            connect(hostAddress, port);
        }
        
        try {
            // データ受信準備
            byte[] buffer = new byte[8192]; // 受信バッファ
            int totalBytesRead = 0;
            boolean stxReceived = false;
            boolean dataComplete = false;
            
            while (!dataComplete) {
                int byteRead = inputStream.read();
                
                if (byteRead == -1) {
                    throw new ZenginCommunicationException("接続が切断されました", "E006");
                }
                
                byte receivedByte = (byte) byteRead;
                
                if (!stxReceived) {
                    if (receivedByte == STX) {
                        stxReceived = true;
                    }
                    continue;
                }
                
                if (receivedByte == ETX) {
                    dataComplete = true;
                    // ACK応答を送信
                    sendControlCharacter(ACK);
                } else {
                    // バッファサイズチェック
                    if (totalBytesRead >= buffer.length) {
                        buffer = Arrays.copyOf(buffer, buffer.length * 2);
                    }
                    buffer[totalBytesRead++] = receivedByte;
                }
            }
            
            // 実際に受信したデータサイズに合わせる
            byte[] receivedData = Arrays.copyOf(buffer, totalBytesRead);
            logger.info("全銀データを受信しました: " + receivedData.length + " バイト");
            
            return receivedData;
            
        } catch (IOException e) {
            throw new ZenginCommunicationException("データ受信中にエラーが発生しました: " + e.getMessage(), e, "E007");
        }
    }
    
    /**
     * 制御文字を送信します
     * 
     * @param controlChar 制御文字
     * @throws IOException 送信エラー発生時
     */
    private void sendControlCharacter(byte controlChar) throws IOException {
        if (outputStream != null) {
            outputStream.write(controlChar);
            outputStream.flush();
            logger.fine("制御文字を送信しました: " + controlChar);
        }
    }
    
    /**
     * 制御文字を読み取ります
     * 
     * @return 読み取った制御文字
     * @throws IOException 読み取りエラー発生時
     * @throws ZenginCommunicationException タイムアウト等のエラー発生時
     */
    private byte readControlCharacter() throws IOException, ZenginCommunicationException {
        if (inputStream != null) {
            int byteRead = inputStream.read();
            if (byteRead == -1) {
                throw new ZenginCommunicationException("接続が切断されました", "E008");
            }
            logger.fine("制御文字を受信しました: " + byteRead);
            return (byte) byteRead;
        }
        throw new ZenginCommunicationException("入力ストリームが初期化されていません", "E009");
    }
    
    /**
     * SSL/TLSソケットファクトリを作成します
     * テスト用にprotectedに変更
     * 
     * @return SSLSocketFactory インスタンス
     * @throws ZenginCommunicationException SSL初期化エラー発生時
     */
    protected SSLSocketFactory getSSLSocketFactory() throws ZenginCommunicationException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            
            // 本番環境では適切な証明書ストアを設定すること
            // ここではデモ用に全ての証明書を信頼するトラストマネージャを使用
            TrustManager[] trustAllCerts = new TrustManager[] { 
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                }
            };
            
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
            
        } catch (Exception e) {
            throw new ZenginCommunicationException("SSL/TLS初期化に失敗しました: " + e.getMessage(), e, "E010");
        }
    }
    
    /**
     * 非TLSソケットを作成します
     * テスト用に追加
     * 
     * @param hostAddress ホストアドレス
     * @param port ポート番号
     * @return Socket インスタンス
     * @throws IOException 接続エラー発生時
     */
    protected Socket createNonTLSSocket(String hostAddress, int port) throws IOException {
        return new Socket(hostAddress, port);
    }
    
    /**
     * 強力な暗号スイートのみをフィルタリングします
     * 
     * @param supportedCipherSuites サポートされている暗号スイート
     * @return 強力な暗号スイートのみ
     */
    private String[] getStrongCipherSuites(String[] supportedCipherSuites) {
        return Arrays.stream(supportedCipherSuites)
                .filter(suite -> 
                    suite.contains("_GCM_") || // GCMモード（推奨）
                    suite.contains("_CHACHA20_") || // ChaCha20（推奨）
                    (suite.contains("_CBC_") && !suite.contains("_SHA_")) // CBCモード（SHA-2以上のみ）
                )
                .filter(suite -> 
                    !suite.contains("_NULL_") && // NULL暗号化を除外
                    !suite.contains("_EXPORT_") && // EXPORT暗号を除外
                    !suite.contains("_anon_") && // 匿名認証を除外
                    !suite.contains("_RC4_") && // RC4を除外
                    !suite.contains("_DES_") && // DESを除外
                    !suite.contains("_3DES_") // 3DESを除外
                )
                .toArray(String[]::new);
    }
    
    /**
     * TLS使用フラグを設定します
     * 
     * @param useTLS TLS使用フラグ
     */
    public void setUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
    }
} 