package com.example.zengin.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * テスト用のTLS設定クラス
 * テスト実行時に必要なキーストアとトラストストアを一時的に作成します
 */
@Configuration
@Profile("test")
public class TestTlsConfig {
    
    @Value("${javax.net.ssl.keyStore:#{null}}")
    private String keyStorePath;
    
    @Value("${javax.net.ssl.keyStorePassword:changeit}")
    private String keyStorePassword;
    
    @Value("${javax.net.ssl.trustStore:#{null}}")
    private String trustStorePath;
    
    @Value("${javax.net.ssl.trustStorePassword:changeit}")
    private String trustStorePassword;
    
    private File tempKeyStoreFile;
    private File tempTrustStoreFile;
    
    /**
     * テスト用の一時的なキーストアとトラストストアを作成します
     */
    @PostConstruct
    public void setupKeyStores() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        // キーストアが設定されていない場合は一時ファイルを作成
        if (keyStorePath == null || keyStorePath.isEmpty()) {
            tempKeyStoreFile = File.createTempFile("test-keystore", ".jks");
            createEmptyKeyStore(tempKeyStoreFile, keyStorePassword);
            System.setProperty("javax.net.ssl.keyStore", tempKeyStoreFile.getAbsolutePath());
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
        }
        
        // トラストストアが設定されていない場合は一時ファイルを作成
        if (trustStorePath == null || trustStorePath.isEmpty()) {
            tempTrustStoreFile = File.createTempFile("test-truststore", ".jks");
            createEmptyKeyStore(tempTrustStoreFile, trustStorePassword);
            System.setProperty("javax.net.ssl.trustStore", tempTrustStoreFile.getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        }
    }
    
    /**
     * テスト終了時に一時ファイルを削除します
     */
    @PreDestroy
    public void cleanupKeyStores() {
        if (tempKeyStoreFile != null && tempKeyStoreFile.exists()) {
            tempKeyStoreFile.delete();
        }
        
        if (tempTrustStoreFile != null && tempTrustStoreFile.exists()) {
            tempTrustStoreFile.delete();
        }
    }
    
    /**
     * 空のキーストアファイルを作成します
     */
    private void createEmptyKeyStore(File keyStoreFile, String password) 
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, password.toCharArray());
        
        try (FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
            keyStore.store(fos, password.toCharArray());
        }
    }
    
    /**
     * テスト用のTLS設定を提供します
     */
    @Bean
    public String tlsTestConfig() {
        return "TLS test configuration is active";
    }
} 