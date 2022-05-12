# ProxyVirtualMotd
プレイヤー別に 仮想 Motd を生成します\
For Minecraft Proxy (BungeeCord / Waterfall) Plugin

`Waterfall 1.18 v483` 動作確認済み

## Features
実装済みの機能です (今後、新機能が追加されるかもしれません)
- 接続元の仮想ホスト名を Motd に表示
- キャッシュ既存プレイヤーのユーザーネームを Motd に表示
- キャッシュ既存プレイヤーのヘッドアイコンをサーバーアイコンに表示

## Configuration
`config.yml`
```yml
motd: '&7Welcome to the &a%virtual_hostname%&7, &e%player_name% &7!' #プレイヤー別に表示される仮想 Motd
to_first_player_text: "Nice to meet you" # プレイヤーキャッシュにデータが存在しない時、%player_name% の部分に表示されるメッセージ
player_favicon_enabled: true #プレイヤー別に表示される当該プレイヤーのヘッドアイコン表示有無
```

## LICENSE
MIT License.