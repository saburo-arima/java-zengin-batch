package com.example.zengin.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * テスト用のSpring Batch設定クラス
 */
@Configuration
@EnableAutoConfiguration
@EnableBatchProcessing
@EntityScan("com.example.zengin")
public class TestBatchConfig {

    /**
     * テスト用のデータソースを設定します
     * 
     * @return テスト用のデータソース
     */
    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:org/springframework/batch/core/schema-h2.sql")
                .build();
    }

    /**
     * テスト用のジョブランチャーユーティリティを設定します
     * 
     * @return ジョブランチャーテストユーティリティ
     */
    @Bean
    public JobLauncherTestUtils jobLauncherTestUtils() {
        return new JobLauncherTestUtils();
    }
} 