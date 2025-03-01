package com.example.zengin.security;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * メッセージの整合性情報を保持するクラス
 * ハッシュ値や件数、金額などの検証情報を管理します
 */
@Entity
@Table(name = "message_integrity_info")
public class MessageIntegrityInfo {
    
    // メッセージID
    @Id
    @Column(name = "message_id", nullable = false)
    private String messageId;
    
    // ハッシュ値
    @Column(name = "hash_value", nullable = false)
    private String hashValue;
    
    // レコード件数
    @Column(name = "record_count", nullable = false)
    private int recordCount;
    
    // 合計金額
    @Column(name = "total_amount", nullable = false)
    private long totalAmount;
    
    // 作成日時
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // 検証結果
    @Column(name = "verified", nullable = false)
    private boolean verified;
    
    /**
     * デフォルトコンストラクタ（JPAのために必要）
     */
    protected MessageIntegrityInfo() {
    }
    
    /**
     * コンストラクタ
     * 
     * @param messageId メッセージID
     * @param hashValue ハッシュ値
     * @param recordCount レコード件数
     * @param totalAmount 合計金額
     */
    public MessageIntegrityInfo(String messageId, String hashValue, int recordCount, long totalAmount) {
        this.messageId = messageId;
        this.hashValue = hashValue;
        this.recordCount = recordCount;
        this.totalAmount = totalAmount;
        this.createdAt = LocalDateTime.now();
        this.verified = false;
    }
    
    /**
     * メッセージIDを取得します
     * 
     * @return メッセージID
     */
    public String getMessageId() {
        return messageId;
    }
    
    /**
     * ハッシュ値を取得します
     * 
     * @return ハッシュ値
     */
    public String getHashValue() {
        return hashValue;
    }
    
    /**
     * レコード件数を取得します
     * 
     * @return レコード件数
     */
    public int getRecordCount() {
        return recordCount;
    }
    
    /**
     * 合計金額を取得します
     * 
     * @return 合計金額
     */
    public long getTotalAmount() {
        return totalAmount;
    }
    
    /**
     * 作成日時を取得します
     * 
     * @return 作成日時
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * 検証結果を取得します
     * 
     * @return 検証結果
     */
    public boolean isVerified() {
        return verified;
    }
    
    /**
     * 検証結果を設定します
     * 
     * @param verified 検証結果
     */
    public void setVerified(boolean verified) {
        this.verified = verified;
    }
    
    @Override
    public String toString() {
        return "MessageIntegrityInfo [messageId=" + messageId + 
               ", hashValue=" + hashValue + 
               ", recordCount=" + recordCount + 
               ", totalAmount=" + totalAmount + 
               ", createdAt=" + createdAt + 
               ", verified=" + verified + "]";
    }
} 