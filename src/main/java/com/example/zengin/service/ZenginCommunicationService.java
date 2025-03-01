package com.example.zengin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.zengin.communication.ZenginCommunicationException;
import com.example.zengin.communication.ZenginTcpIpProtocol;
import com.example.zengin.format.ZenginMessage;
import com.example.zengin.format.ZenginMessage.MessageType;
import com.example.zengin.security.MessageIntegrityService;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 全銀通信サービス
 * 全銀TCP/IPプロトコルを使用した銀行との通信処理を提供します
 */
@Service
public class ZenginCommunicationService {
    
    private static final Logger logger = Logger.getLogger(ZenginCommunicationService.class.getName());
    
    @Autowired
    private ZenginTcpIpProtocol zenginProtocol;
    
    @Autowired
    private MessageIntegrityService integrityService;
    
    @Value("${zengin.bank.host:localhost}")
    private String bankHost;
    
    @Value("${zengin.bank.port:20000}")
    private int bankPort;
    
    @Value("${zengin.sender.id:TESTSENDER}")
    private String senderId;
    
    @Value("${zengin.integrity.check.enabled:true}")
    private boolean integrityCheckEnabled;
    
    /**
     * 振込データを送信します
     * 
     * @param receiverId 受信者ID（銀行ID）
     * @param transferData 振込データ（全銀フォーマット準拠）
     * @return 送信結果
     * @throws ZenginCommunicationException 通信エラー発生時
     */
    public boolean sendTransferData(String receiverId, byte[][] transferData) throws ZenginCommunicationException {
        logger.info("振込データ送信を開始します: 送信先=" + receiverId);
        
        try {
            // 全銀メッセージを作成
            ZenginMessage message = new ZenginMessage(MessageType.TRANSFER, senderId, receiverId);
            message.setDataRecords(transferData);
            
            // メッセージの整合性情報を生成・保存（有効な場合）
            if (integrityCheckEnabled) {
                integrityService.generateAndSaveIntegrityInfo(message);
            }
            
            // 全銀プロトコルでデータ送信
            boolean result = zenginProtocol.sendData(bankHost, bankPort, message.toByteArray());
            
            logger.info("振込データ送信が" + (result ? "成功" : "失敗") + "しました");
            return result;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "振込データ送信中にエラーが発生しました", e);
            throw new ZenginCommunicationException("振込データ送信に失敗しました: " + e.getMessage(), e);
        }
    }
    
    /**
     * 照会データを送信し、応答を受信します
     * 
     * @param receiverId 受信者ID（銀行ID）
     * @param inquiryData 照会データ（全銀フォーマット準拠）
     * @return 応答データ
     * @throws ZenginCommunicationException 通信エラー発生時
     */
    public ZenginMessage sendInquiryAndReceiveResponse(String receiverId, byte[][] inquiryData) 
            throws ZenginCommunicationException {
        logger.info("照会データ送信を開始します: 送信先=" + receiverId);
        
        try {
            // 全銀メッセージを作成
            ZenginMessage message = new ZenginMessage(MessageType.INQUIRY, senderId, receiverId);
            message.setDataRecords(inquiryData);
            
            // メッセージの整合性情報を生成・保存（有効な場合）
            if (integrityCheckEnabled) {
                integrityService.generateAndSaveIntegrityInfo(message);
            }
            
            // 全銀プロトコルでデータ送信
            boolean sendResult = zenginProtocol.sendData(bankHost, bankPort, message.toByteArray());
            if (!sendResult) {
                throw new ZenginCommunicationException("照会データの送信に失敗しました");
            }
            
            logger.info("照会データを送信しました。応答を待機します...");
            
            // 応答データを受信
            byte[] responseData = zenginProtocol.receiveData(bankHost, bankPort);
            ZenginMessage responseMessage = ZenginMessage.fromByteArray(responseData);
            
            if (responseMessage.getMessageType() != MessageType.RESPONSE) {
                throw new ZenginCommunicationException(
                    "不正な応答種別を受信しました: " + responseMessage.getMessageType()
                );
            }
            
            // 受信メッセージの整合性を検証（有効な場合）
            if (integrityCheckEnabled) {
                // 重複メッセージのチェック
                if (integrityService.isDuplicateMessage(responseMessage.getFileId())) {
                    throw new ZenginCommunicationException("重複したメッセージを受信しました: " + responseMessage.getFileId());
                }
                
                // 整合性情報を生成・保存
                integrityService.generateAndSaveIntegrityInfo(responseMessage);
                
                // トレーラレコードの整合性を検証
                verifyTrailerConsistency(responseMessage);
            }
            
            logger.info("照会応答を受信しました: 送信元=" + responseMessage.getSenderId());
            return responseMessage;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "照会処理中にエラーが発生しました", e);
            throw new ZenginCommunicationException("照会処理に失敗しました: " + e.getMessage(), e);
        }
    }
    
    /**
     * 通知データを受信します
     * 
     * @return 受信した通知データ
     * @throws ZenginCommunicationException 通信エラー発生時
     */
    public ZenginMessage receiveNotification() throws ZenginCommunicationException {
        logger.info("通知データ受信を開始します");
        
        try {
            // 通知データを受信
            byte[] notificationData = zenginProtocol.receiveData(bankHost, bankPort);
            ZenginMessage notificationMessage = ZenginMessage.fromByteArray(notificationData);
            
            if (notificationMessage.getMessageType() != MessageType.NOTIFICATION) {
                throw new ZenginCommunicationException(
                    "不正な通知種別を受信しました: " + notificationMessage.getMessageType()
                );
            }
            
            // 受信メッセージの整合性を検証（有効な場合）
            if (integrityCheckEnabled) {
                // 重複メッセージのチェック
                if (integrityService.isDuplicateMessage(notificationMessage.getFileId())) {
                    throw new ZenginCommunicationException("重複したメッセージを受信しました: " + notificationMessage.getFileId());
                }
                
                // 整合性情報を生成・保存
                integrityService.generateAndSaveIntegrityInfo(notificationMessage);
                
                // トレーラレコードの整合性を検証
                verifyTrailerConsistency(notificationMessage);
            }
            
            logger.info("通知データを受信しました: 送信元=" + notificationMessage.getSenderId());
            return notificationMessage;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "通知データ受信中にエラーが発生しました", e);
            throw new ZenginCommunicationException("通知データ受信に失敗しました: " + e.getMessage(), e);
        }
    }
    
    /**
     * トレーラレコードの整合性を検証します
     * 
     * @param message 検証する全銀メッセージ
     * @throws ZenginCommunicationException 検証に失敗した場合
     */
    private void verifyTrailerConsistency(ZenginMessage message) throws ZenginCommunicationException {
        if (message == null || message.getDataRecords() == null) {
            return;
        }
        
        try {
            // トレーラレコードからレコード件数を取得
            byte[] trailerRecord = message.toByteArray();
            int trailerOffset = trailerRecord.length - 80; // トレーラレコードの開始位置
            
            byte[] recordCountBytes = new byte[8];
            System.arraycopy(trailerRecord, trailerOffset + 6, recordCountBytes, 0, 8);
            String recordCountStr = new String(recordCountBytes, StandardCharsets.UTF_8).trim();
            int expectedRecordCount = Integer.parseInt(recordCountStr);
            
            // 実際のレコード件数
            int actualRecordCount = message.getDataRecords().length;
            
            if (expectedRecordCount != actualRecordCount) {
                throw new ZenginCommunicationException(
                    "トレーラレコードのレコード件数が一致しません: 期待値=" + expectedRecordCount + ", 実際=" + actualRecordCount
                );
            }
            
            logger.info("トレーラレコードの整合性検証に成功しました");
            
        } catch (NumberFormatException e) {
            throw new ZenginCommunicationException("トレーラレコードの解析に失敗しました: " + e.getMessage(), e);
        }
    }
    
    /**
     * 銀行ホスト設定を取得します
     * 
     * @return 銀行ホスト
     */
    public String getBankHost() {
        return bankHost;
    }
    
    /**
     * 銀行ホスト設定を設定します
     * 
     * @param bankHost 銀行ホスト
     */
    public void setBankHost(String bankHost) {
        this.bankHost = bankHost;
    }
    
    /**
     * 銀行ポート設定を取得します
     * 
     * @return 銀行ポート
     */
    public int getBankPort() {
        return bankPort;
    }
    
    /**
     * 銀行ポート設定を設定します
     * 
     * @param bankPort 銀行ポート
     */
    public void setBankPort(int bankPort) {
        this.bankPort = bankPort;
    }
    
    /**
     * 整合性チェック有効フラグを取得します
     * 
     * @return 整合性チェック有効フラグ
     */
    public boolean isIntegrityCheckEnabled() {
        return integrityCheckEnabled;
    }
    
    /**
     * 整合性チェック有効フラグを設定します
     * 
     * @param integrityCheckEnabled 整合性チェック有効フラグ
     */
    public void setIntegrityCheckEnabled(boolean integrityCheckEnabled) {
        this.integrityCheckEnabled = integrityCheckEnabled;
    }
} 