package com.example.zengin.security;

import java.util.List;
import java.util.Optional;

/**
 * メッセージ整合性情報を管理するリポジトリインターフェース
 * 整合性情報の保存や検索機能を提供します
 */
public interface MessageIntegrityRepository {
    
    /**
     * メッセージ整合性情報を保存します
     * 
     * @param integrityInfo 保存する整合性情報
     * @return 保存された整合性情報
     */
    MessageIntegrityInfo save(MessageIntegrityInfo integrityInfo);
    
    /**
     * メッセージIDで整合性情報を検索します
     * 
     * @param messageId 検索するメッセージID
     * @return 見つかった整合性情報（存在しない場合は空）
     */
    Optional<MessageIntegrityInfo> findByMessageId(String messageId);
    
    /**
     * 全ての整合性情報を取得します
     * 
     * @return 整合性情報のリスト
     */
    List<MessageIntegrityInfo> findAll();
    
    /**
     * 指定した日時以降の整合性情報を取得します
     * 
     * @param days 取得する日数
     * @return 整合性情報のリスト
     */
    List<MessageIntegrityInfo> findRecentEntries(int days);
    
    /**
     * メッセージIDで整合性情報を削除します
     * 
     * @param messageId 削除するメッセージID
     * @return 削除に成功した場合はtrue、それ以外はfalse
     */
    boolean deleteByMessageId(String messageId);
} 