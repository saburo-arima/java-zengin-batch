package com.example.zengin.security;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.zengin.communication.ZenginCommunicationException;
import com.example.zengin.format.ZenginMessage;

/**
 * メッセージ整合性チェックサービス
 * 全銀メッセージの整合性検証機能を提供します
 */
@Service
public class MessageIntegrityService {
    
    private static final Logger logger = Logger.getLogger(MessageIntegrityService.class.getName());
    
    // 全銀フォーマットの金額フィールド位置（仮の値、実際のフォーマットに合わせて調整が必要）
    private static final int AMOUNT_FIELD_START_POS = 30;
    private static final int AMOUNT_FIELD_LENGTH = 12;
    
    @Autowired
    private MessageIntegrityUtil integrityUtil;
    
    @Autowired
    private MessageIntegrityRepository integrityRepository;
    
    /**
     * メッセージの整合性情報を生成し、保存します
     * 
     * @param message 全銀メッセージ
     * @return 生成された整合性情報
     * @throws ZenginCommunicationException 整合性情報の生成に失敗した場合
     */
    public MessageIntegrityInfo generateAndSaveIntegrityInfo(ZenginMessage message) throws ZenginCommunicationException {
        try {
            // メッセージのバイト配列を取得
            byte[] messageData = message.toByteArray();
            
            // ハッシュ値を計算
            String hashValue = integrityUtil.calculateHash(messageData);
            
            // レコード件数を計算
            int recordCount = integrityUtil.calculateRecordCount(message.getDataRecords());
            
            // 合計金額を計算
            long totalAmount = integrityUtil.calculateTotalAmount(
                    message.getDataRecords(), 
                    AMOUNT_FIELD_START_POS, 
                    AMOUNT_FIELD_LENGTH
            );
            
            // メッセージIDを生成（ファイルIDを使用）
            String messageId = message.getFileId();
            
            // 整合性情報を生成
            MessageIntegrityInfo integrityInfo = new MessageIntegrityInfo(
                    messageId, 
                    hashValue, 
                    recordCount, 
                    totalAmount
            );
            
            // 整合性情報を保存
            integrityRepository.save(integrityInfo);
            
            logger.info("メッセージ整合性情報を生成しました: " + integrityInfo);
            return integrityInfo;
            
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "ハッシュ計算中にエラーが発生しました", e);
            throw new ZenginCommunicationException("メッセージ整合性情報の生成に失敗しました: " + e.getMessage(), e);
        }
    }
    
    /**
     * メッセージの整合性を検証します
     * 
     * @param message 検証する全銀メッセージ
     * @return 検証結果（成功した場合はtrue、それ以外はfalse）
     * @throws ZenginCommunicationException 検証中にエラーが発生した場合
     */
    public boolean verifyMessageIntegrity(ZenginMessage message) throws ZenginCommunicationException {
        try {
            // メッセージIDを取得
            String messageId = message.getFileId();
            
            // 保存された整合性情報を検索
            Optional<MessageIntegrityInfo> savedInfoOpt = integrityRepository.findByMessageId(messageId);
            
            if (!savedInfoOpt.isPresent()) {
                logger.warning("メッセージID " + messageId + " の整合性情報が見つかりません");
                return false;
            }
            
            MessageIntegrityInfo savedInfo = savedInfoOpt.get();
            
            // メッセージのバイト配列を取得
            byte[] messageData = message.toByteArray();
            
            // ハッシュ値を検証
            boolean hashVerified = integrityUtil.verifyHash(messageData, savedInfo.getHashValue());
            
            if (!hashVerified) {
                logger.warning("メッセージID " + messageId + " のハッシュ値が一致しません");
                return false;
            }
            
            // レコード件数を検証
            int actualRecordCount = integrityUtil.calculateRecordCount(message.getDataRecords());
            boolean recordCountVerified = (actualRecordCount == savedInfo.getRecordCount());
            
            if (!recordCountVerified) {
                logger.warning("メッセージID " + messageId + " のレコード件数が一致しません: " + 
                        "期待値=" + savedInfo.getRecordCount() + ", 実際=" + actualRecordCount);
                return false;
            }
            
            // 合計金額を検証
            long actualTotalAmount = integrityUtil.calculateTotalAmount(
                    message.getDataRecords(), 
                    AMOUNT_FIELD_START_POS, 
                    AMOUNT_FIELD_LENGTH
            );
            boolean totalAmountVerified = (actualTotalAmount == savedInfo.getTotalAmount());
            
            if (!totalAmountVerified) {
                logger.warning("メッセージID " + messageId + " の合計金額が一致しません: " + 
                        "期待値=" + savedInfo.getTotalAmount() + ", 実際=" + actualTotalAmount);
                return false;
            }
            
            // 検証結果を更新
            savedInfo.setVerified(true);
            integrityRepository.save(savedInfo);
            
            logger.info("メッセージID " + messageId + " の整合性検証に成功しました");
            return true;
            
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "ハッシュ検証中にエラーが発生しました", e);
            throw new ZenginCommunicationException("メッセージ整合性検証に失敗しました: " + e.getMessage(), e);
        }
    }
    
    /**
     * トレーラレコードの情報と実際のデータを比較して整合性を検証します
     * 
     * @param message 検証する全銀メッセージ
     * @param expectedRecordCount トレーラレコードに記載されたレコード件数
     * @param expectedTotalAmount トレーラレコードに記載された合計金額
     * @return 検証結果（成功した場合はtrue、それ以外はfalse）
     */
    public boolean verifyTrailerConsistency(ZenginMessage message, int expectedRecordCount, long expectedTotalAmount) {
        // 実際のレコード件数を計算
        int actualRecordCount = integrityUtil.calculateRecordCount(message.getDataRecords());
        
        // 実際の合計金額を計算
        long actualTotalAmount = integrityUtil.calculateTotalAmount(
                message.getDataRecords(), 
                AMOUNT_FIELD_START_POS, 
                AMOUNT_FIELD_LENGTH
        );
        
        // レコード件数を検証
        boolean recordCountVerified = (actualRecordCount == expectedRecordCount);
        
        if (!recordCountVerified) {
            logger.warning("トレーラレコードのレコード件数が一致しません: " + 
                    "期待値=" + expectedRecordCount + ", 実際=" + actualRecordCount);
            return false;
        }
        
        // 合計金額を検証
        boolean totalAmountVerified = (actualTotalAmount == expectedTotalAmount);
        
        if (!totalAmountVerified) {
            logger.warning("トレーラレコードの合計金額が一致しません: " + 
                    "期待値=" + expectedTotalAmount + ", 実際=" + actualTotalAmount);
            return false;
        }
        
        logger.info("トレーラレコードの整合性検証に成功しました");
        return true;
    }
    
    /**
     * メッセージIDの重複をチェックします
     * 
     * @param messageId チェックするメッセージID
     * @return 重複している場合はtrue、それ以外はfalse
     */
    public boolean isDuplicateMessage(String messageId) {
        Optional<MessageIntegrityInfo> existingInfo = integrityRepository.findByMessageId(messageId);
        
        if (existingInfo.isPresent()) {
            logger.warning("メッセージID " + messageId + " は既に処理されています");
            return true;
        }
        
        return false;
    }
} 