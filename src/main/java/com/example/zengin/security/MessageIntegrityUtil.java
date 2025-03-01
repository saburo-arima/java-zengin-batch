package com.example.zengin.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * メッセージの整合性チェックを行うユーティリティクラス
 * ハッシュ値の計算や検証機能を提供します
 */
@Component
public class MessageIntegrityUtil {
    
    // ハッシュアルゴリズム
    private static final String HASH_ALGORITHM = "SHA-256";
    
    /**
     * データのハッシュ値を計算します
     * 
     * @param data ハッシュ値を計算するデータ
     * @return ハッシュ値（16進数文字列）
     * @throws NoSuchAlgorithmException ハッシュアルゴリズムが存在しない場合
     */
    public String calculateHash(byte[] data) throws NoSuchAlgorithmException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("ハッシュ計算対象のデータが空です");
        }
        
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] hashBytes = digest.digest(data);
        
        return bytesToHex(hashBytes);
    }
    
    /**
     * データのハッシュ値を計算し、期待されるハッシュ値と比較します
     * 
     * @param data 検証対象のデータ
     * @param expectedHash 期待されるハッシュ値
     * @return ハッシュ値が一致する場合はtrue、それ以外はfalse
     * @throws NoSuchAlgorithmException ハッシュアルゴリズムが存在しない場合
     */
    public boolean verifyHash(byte[] data, String expectedHash) throws NoSuchAlgorithmException {
        if (expectedHash == null || expectedHash.isEmpty()) {
            return false;
        }
        
        String actualHash = calculateHash(data);
        return actualHash.equalsIgnoreCase(expectedHash);
    }
    
    /**
     * バイト配列を16進数文字列に変換します
     * 
     * @param bytes 変換対象のバイト配列
     * @return 16進数文字列
     */
    private String bytesToHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
    
    /**
     * 16進数文字列をバイト配列に変換します
     * 
     * @param hexString 変換対象の16進数文字列
     * @return バイト配列
     */
    public byte[] hexToBytes(String hexString) {
        return HexFormat.of().parseHex(hexString);
    }
    
    /**
     * データレコードの合計件数を計算します
     * 
     * @param dataRecords データレコード配列
     * @return データレコードの合計件数
     */
    public int calculateRecordCount(byte[][] dataRecords) {
        return dataRecords != null ? dataRecords.length : 0;
    }
    
    /**
     * データレコードから合計金額を計算します
     * 金額フィールドの位置と長さは全銀フォーマットに依存します
     * 
     * @param dataRecords データレコード配列
     * @param amountStartPos 金額フィールドの開始位置（0ベース）
     * @param amountLength 金額フィールドの長さ
     * @return 合計金額
     */
    public long calculateTotalAmount(byte[][] dataRecords, int amountStartPos, int amountLength) {
        if (dataRecords == null || dataRecords.length == 0) {
            return 0;
        }
        
        long totalAmount = 0;
        
        for (byte[] record : dataRecords) {
            if (record != null && record.length > amountStartPos + amountLength) {
                byte[] amountBytes = Arrays.copyOfRange(record, amountStartPos, amountStartPos + amountLength);
                String amountStr = new String(amountBytes, StandardCharsets.UTF_8).trim();
                
                try {
                    long amount = Long.parseLong(amountStr);
                    totalAmount += amount;
                } catch (NumberFormatException e) {
                    // 金額フィールドが数値でない場合はスキップ
                }
            }
        }
        
        return totalAmount;
    }
} 