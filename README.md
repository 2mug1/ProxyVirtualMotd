# ProxyVirtualMotd
[![Build](https://github.com/takagi-minecraft-lab/ProxyVirtualMotd/actions/workflows/build.yml/badge.svg)](https://github.com/takagi-minecraft-lab/ProxyVirtualMotd/actions/workflows/build.yml)

プレイヤー別に 仮想 Motd を表示するプラグインです\
For Minecraft Proxy (BungeeCord / Waterfall) Plugin

`Waterfall 1.18 v483` 動作確認済み

## Features
実装済みの機能です (今後、新機能が追加されるかもしれません)
- デフォルトの Motd を表示
- 接続元の仮想ホスト名を Motd に表示
- キャッシュ既存プレイヤーのユーザーネームを Motd に表示
- キャッシュ既存プレイヤーのヘッドアイコンをサーバーアイコンに表示

## Configuration
`config.yml`
```yml
default_motd: '&7Welcome to the &amyserver.com &7!' # デフォルトで表示される Motd
player_motd: '&7Welcome to the &a%virtual_hostname%&7, &e%player_name% &7!' #プレイヤー別に表示される仮想 Motd
player_favicon_enabled: false #プレイヤー別に表示される当該プレイヤーのヘッドアイコン表示有無
```

## Build
`$ mvn clean package`

## LICENSE
MIT License.
