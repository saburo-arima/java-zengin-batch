package com.example.zengin.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * 全銀TCP/IP通信のTLS設定クラス
 * SSL/TLS暗号化通信の設定を行います
 */
@Configuration
public class ZenginTlsConfig {
    
    @Value("${zengin.tls.keystore.path:#{null}}")
    private String keystorePath;
    
    @Value("${zengin.tls.keystore.password:#{null}}")
    private String keystorePassword;
    
    @Value("${zengin.tls.truststore.path:#{null}}")
    private String truststorePath;
    
    @Value("${zengin.tls.truststore.password:#{null}}")
    private String truststorePassword;
    
    @Value("${zengin.tls.enabled:true}")
    private boolean tlsEnabled;
    
    private final ResourceLoader resourceLoader;
    
    /**
     * コンストラクタ
     * 
     * @param resourceLoader リソースローダー
     */
    public ZenginTlsConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    /**
     * SSLコンテキストを生成します
     * 
     * @return 設定済みのSSLContext
     * @throws Exception 初期化エラー発生時
     */
    @Bean
    public SSLContext sslContext() throws Exception {
        if (!tlsEnabled) {
            return null;
        }
        
        try {
            // TLS 1.2以上を使用
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            
            // キーストアが設定されている場合は読み込む
            KeyManagerFactory keyManagerFactory = null;
            if (keystorePath != null && keystorePassword != null) {
                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                KeyStore keyStore = loadKeyStore(keystorePath, keystorePassword);
                keyManagerFactory.init(keyStore, keystorePassword.toCharArray());
            }
            
            // トラストストアが設定されている場合は読み込む
            TrustManagerFactory trustManagerFactory = null;
            if (truststorePath != null && truststorePassword != null) {
                trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                KeyStore trustStore = loadKeyStore(truststorePath, truststorePassword);
                trustManagerFactory.init(trustStore);
            }
            
            // SSLコンテキストを初期化
            sslContext.init(
                keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null,
                null
            );
            
            return sslContext;
            
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | 
                 IOException | UnrecoverableKeyException | KeyManagementException e) {
            throw new Exception("SSL/TLS設定の初期化に失敗しました: " + e.getMessage(), e);
        }
    }
    
    /**
     * キーストアをロードします
     * 
     * @param path キーストアのパス
     * @param password キーストアのパスワード
     * @return ロードされたKeyStoreオブジェクト
     * @throws KeyStoreException キーストアエラー発生時
     * @throws NoSuchAlgorithmException アルゴリズムエラー発生時
     * @throws CertificateException 証明書エラー発生時
     * @throws IOException 入出力エラー発生時
     */
    private KeyStore loadKeyStore(String path, String password) 
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Resource resource = resourceLoader.getResource(path);
        try (InputStream is = resource.getInputStream()) {
            keyStore.load(is, password.toCharArray());
        }
        return keyStore;
    }
    
    /**
     * TLS有効フラグを取得します
     * 
     * @return TLS有効フラグ
     */
    public boolean isTlsEnabled() {
        return tlsEnabled;
    }
} 