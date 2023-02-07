# Simple App Blocker

[![Weekly Build](https://github.com/CASL0/simple_app_blocker/actions/workflows/weekly_build.yml/badge.svg)](https://github.com/CASL0/simple_app_blocker/actions/workflows/weekly_build.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![API](https://img.shields.io/badge/API-29%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=29)

[VpnService](https://developer.android.com/reference/android/net/VpnService)を利用し、指定のアプリ以外の通信をブロックするシンプルなユーティリティツールです。

対象 OS：Android10+

<a href="https://play.google.com/store/apps/details?id=jp.co.casl0.android.simpleappblocker&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1"><img height="80" alt="Google Play で手に入れよう" src="https://play.google.com/intl/en_us/badges/static/images/badges/ja_badge_web_generic.png"/></a>

## 機能

- ブロックした通信をリアルタイムで表示
- アプリ指定で通信許可
- IPv4/IPv6 対応

## 開発

### 開発環境

Android Studio (Electric Eel 以降) をインストールしてください。

- Kotlin 1.7.10
- Android Gradle Plugin 7.4.1
- Gradle 8.0-rc-2
- JDK 19
- NDK 25.2.9519653

以下のコマンドを実行し、JNI から使用しているネイティブライブラリ（Pcap++）をセットアップしてください。

```bash
./gradlew setup
```

## ドキュメント

- [docs](/docs)

※ Android Studio の[Markdown](https://pleiades.io/help/idea/markdown.html)プラグインでプレビューするには、JRE を JCEF に切り替えてください。
また、PlantUML ダイアグラムを有効にするために`Languages & Frameworks` > `Markdown`から PlantUML 拡張を有効にしてください。

## ライセンス

```
Copyright 2022 CASL0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
