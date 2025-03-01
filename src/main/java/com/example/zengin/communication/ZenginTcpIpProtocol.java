package com.example.zengin.communication;

/**
 * 全銀TCP/IPプロトコルの通信インターフェース
 * 全銀協標準通信プロトコルに準拠した送受信処理を定義します
 */
public interface ZenginTcpIpProtocol {
    
    /**
     * 銀行ホストに接続し、データを送信します
     * 
     * @param hostAddress 接続先ホストアドレス
     * @param port 接続先ポート
     * @param data 送信データ（全銀フォーマット）
     * @return 送信結果
     * @throws ZenginCommunicationException 通信エラー発生時
     */
    boolean sendData(String hostAddress, int port, byte[] data) throws ZenginCommunicationException;
    
    /**
     * 銀行ホストからデータを受信します
     * 
     * @param hostAddress 接続先ホストアドレス
     * @param port 接続先ポート
     * @return 受信データ（全銀フォーマット）
     * @throws ZenginCommunicationException 通信エラー発生時
     */
    byte[] receiveData(String hostAddress, int port) throws ZenginCommunicationException;
    
    /**
     * 通信接続を確立します
     * 
     * @param hostAddress 接続先ホストアドレス
     * @param port 接続先ポート
     * @throws ZenginCommunicationException 接続エラー発生時
     */
    void connect(String hostAddress, int port) throws ZenginCommunicationException;
    
    /**
     * 通信接続を切断します
     * 
     * @throws ZenginCommunicationException 切断エラー発生時
     */
    void disconnect() throws ZenginCommunicationException;
} 