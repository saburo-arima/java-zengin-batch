package com.example.zengin.security;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * メッセージ整合性情報を管理するリポジトリインターフェース
 * 整合性情報の保存や検索機能を提供します
 */
@Repository
public interface MessageIntegrityRepository extends JpaRepository<MessageIntegrityInfo, String> {
    
    /**
     * メッセージIDで整合性情報を検索します
     * 
     * @param messageId 検索するメッセージID
     * @return 見つかった整合性情報（存在しない場合は空）
     */
    Optional<MessageIntegrityInfo> findByMessageId(String messageId);
    
    /**
     * 指定した日時以降の整合性情報を取得します
     * 
     * @param cutoffDate 基準日時
     * @return 整合性情報のリスト
     */
    @Query("SELECT m FROM MessageIntegrityInfo m WHERE m.createdAt > :cutoffDate")
    List<MessageIntegrityInfo> findByCreatedAtAfter(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 指定した日数以内の整合性情報を取得します
     * 
     * @param days 取得する日数
     * @return 整合性情報のリスト
     */
    default List<MessageIntegrityInfo> findRecentEntries(int days) {
        if (days <= 0) {
            return List.of();
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return findByCreatedAtAfter(cutoffDate);
    }
    
    /**
     * メッセージIDで整合性情報を削除します
     * 
     * @param messageId 削除するメッセージID
     * @return 削除に成功した場合はtrue、それ以外はfalse
     */
    default boolean deleteByMessageId(String messageId) {
        if (messageId == null) {
            return false;
        }
        
        if (existsById(messageId)) {
            deleteById(messageId);
            return true;
        }
        
        return false;
    }
} 