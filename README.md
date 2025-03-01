# Java Zengin Batch

**注意**: 本アプリケーションは Cursor を用いて作成された実験的モジュールです。実際の金融システムでの使用は想定されていません。

## 概要

Java Zengin Batch は、日本の銀行間決済システムである全銀システム（Zengin System）と通信するための Java Spring Boot アプリケーションです。このアプリケーションは、全銀フォーマットのメッセージ処理、TCP/IP 通信、セキュリティ機能を提供します。

## 主な機能

- **全銀フォーマットメッセージの処理**: 全銀フォーマットに準拠したメッセージの生成と解析
- **TCP/IP 通信**: 全銀システムとの TCP/IP プロトコルによる通信
- **セキュリティ機能**:
  - TLS 暗号化通信
  - メッセージ整合性チェック（ハッシュ値検証）
  - 重複メッセージ検出
  - トレーラーレコード検証
- **データ永続化**: H2 データベースを使用したメッセージと整合性情報の保存

## 技術スタック

- Java 21
- Spring Boot 3.2.0
- Spring Batch
- Spring Integration
- Spring Data JPA
- H2 Database

## プロジェクト構成

```
src/main/java/com/example/zengin/
├── ZenginBatchApplication.java      # アプリケーションのエントリーポイント
├── message/                         # メッセージ処理関連
│   ├── ZenginMessage.java           # 全銀メッセージモデル
│   └── ZenginMessageFormat.java     # メッセージフォーマット処理
├── communication/                   # 通信関連
│   ├── ZenginCommunicationService.java  # 通信サービス
│   └── ZenginTcpIpProtocol.java     # TCP/IPプロトコル実装
├── security/                        # セキュリティ関連
│   ├── MessageIntegrityService.java # メッセージ整合性サービス
│   ├── MessageIntegrityInfo.java    # 整合性情報モデル
│   ├── MessageIntegrityRepository.java  # 整合性情報リポジトリ
│   └── MessageIntegrityUtil.java    # ユーティリティクラス
└── config/                          # 設定関連
    └── ZenginTlsConfig.java         # TLS設定
```

## 使用方法

### アプリケーションの実行

#### Gradle を使用して実行

```bash
./gradlew bootRun
```

#### JAR ファイルをビルドして実行

```bash
./gradlew build
java -jar build/libs/java-zengin-batch-0.0.1-SNAPSHOT.jar
```

### 設定のカスタマイズ

`src/main/resources/application.properties` ファイルで以下の設定をカスタマイズできます：

- データベース接続設定
- 全銀通信設定（ホスト、ポート、送信者ID）
- TLS設定（キーストア、トラストストアのパスとパスワード）
- メッセージ整合性チェック設定

例：

```properties
# 全銀通信設定
zengin.bank.host=localhost
zengin.bank.port=20000
zengin.sender.id=1234567890

# TLS設定
zengin.tls.enabled=true
zengin.tls.keystore-path=classpath:keystore/zengin-keystore.p12
zengin.tls.keystore-password=password
zengin.tls.truststore-path=classpath:keystore/zengin-truststore.p12
zengin.tls.truststore-password=password
```

### 実行時の設定変更

コマンドライン引数を使用して設定を上書きできます：

```bash
java -jar build/libs/java-zengin-batch-0.0.1-SNAPSHOT.jar --zengin.bank.host=real-bank-host --zengin.bank.port=5000
```

## 注意事項

1. このアプリケーションはテスト・学習目的で作成されています。実際の全銀システムとの接続には、金融機関から提供される正式な接続情報や証明書が必要です。

2. デフォルトでは H2 インメモリデータベースを使用しています。本番環境では適切なデータベース（MySQL や PostgreSQL など）に変更することをお勧めします。

3. セキュリティ上の理由から、実際の運用では適切なキーストアとトラストストアを使用し、パスワードは環境変数などで安全に管理してください。

## ライセンス

このプロジェクトは MIT ライセンスの下で公開されています。詳細については LICENSE ファイルを参照してください。 