package com.example.zengin.security;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

/**
 * メッセージ整合性情報を管理するリポジトリのインメモリ実装
 * 実際の運用ではデータベースに保存する実装に置き換えることを想定しています
 */
@Repository
public class InMemoryMessageIntegrityRepository implements MessageIntegrityRepository {
    
    // メッセージIDをキーとした整合性情報のマップ
    private final Map<String, MessageIntegrityInfo> integrityInfoMap = new ConcurrentHashMap<>();
    
    @Override
    public MessageIntegrityInfo save(MessageIntegrityInfo integrityInfo) {
        if (integrityInfo == null || integrityInfo.getMessageId() == null) {
            throw new IllegalArgumentException("整合性情報またはメッセージIDがnullです");
        }
        
        integrityInfoMap.put(integrityInfo.getMessageId(), integrityInfo);
        return integrityInfo;
    }
    
    @Override
    public Optional<MessageIntegrityInfo> findByMessageId(String messageId) {
        if (messageId == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(integrityInfoMap.get(messageId));
    }
    
    @Override
    public List<MessageIntegrityInfo> findAll() {
        return new ArrayList<>(integrityInfoMap.values());
    }
    
    @Override
    public List<MessageIntegrityInfo> findRecentEntries(int days) {
        if (days <= 0) {
            return new ArrayList<>();
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        return integrityInfoMap.values().stream()
                .filter(info -> info.getCreatedAt().isAfter(cutoffDate))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean deleteByMessageId(String messageId) {
        if (messageId == null) {
            return false;
        }
        
        return integrityInfoMap.remove(messageId) != null;
    }
} 