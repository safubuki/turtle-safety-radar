package com.turtlesafety.radar.parent.guard

/**
 * 仕様書 v0.4 §6 で定義する External Guard チェックリストの静的定義。
 *
 * このアプリ単体で SNS / ブラウザ / アプリ追加などを完全制御することはできないため、
 * 保護者が外部のペアレンタルコントロールや端末設定を「実際に併用しているか」を
 * 自己点検できるようにする。
 *
 * - 項目順は仕様書の節順
 * - id は SharedPreferences の永続化キーとして使うため、絶対に変えない
 *   (項目を増やすときは新規 id を採番すること)
 */
data class ChecklistCategory(
    val id: String,
    val title: String,
    val description: String,
    val items: List<ChecklistItem>,
)

data class ChecklistItem(
    val id: String,
    val label: String,
    val hint: String? = null,
)

object ChecklistDefinitions {

    val categories: List<ChecklistCategory> = listOf(
        ChecklistCategory(
            id = "apps",
            title = "アプリ制限",
            description = "子どもが触れる SNS / ブラウザ / ストア の制限状態を確認します。",
            items = listOf(
                ChecklistItem("apps_line", "LINE の利用ルールを決めている"),
                ChecklistItem("apps_openchat", "LINE オープンチャットを許可するか方針が決まっている"),
                ChecklistItem("apps_browser", "ブラウザの利用範囲を決めている"),
                ChecklistItem(
                    "apps_sns",
                    "Instagram / X / TikTok / Discord を許可するか決めている",
                ),
                ChecklistItem("apps_game_chat", "ゲーム内チャットを許可するか決めている"),
                ChecklistItem(
                    "apps_playstore",
                    "Play ストアの購入・インストールを制限している",
                ),
                ChecklistItem(
                    "apps_unknown_sources",
                    "「不明なアプリのインストール」を OFF にしている",
                ),
            ),
        ),
        ChecklistCategory(
            id = "guard",
            title = "設定変更対策",
            description = "子どもが本アプリや権限を勝手に解除できないように設定しているか確認します。",
            items = listOf(
                ChecklistItem(
                    "guard_settings",
                    "設定アプリへの自由なアクセスを制限している",
                ),
                ChecklistItem(
                    "guard_notif_listener",
                    "通知監視権限を解除しにくくしている",
                ),
                ChecklistItem(
                    "guard_ime_switch",
                    "IME 切り替えを簡単にできないようにしている",
                ),
                ChecklistItem(
                    "guard_vpn",
                    "VPN / プロキシを勝手に追加できないようにしている",
                ),
                ChecklistItem(
                    "guard_install",
                    "新規アプリのインストールを制限している",
                ),
            ),
        ),
        ChecklistCategory(
            id = "contact",
            title = "連絡手段対策",
            description = "電話・メール・DM・ゲーム内チャットなどの連絡手段の方針を確認します。",
            items = listOf(
                ChecklistItem("contact_call", "通話先制限を設定している"),
                ChecklistItem("contact_sms", "SMS 利用を許可するか決めている"),
                ChecklistItem("contact_mail", "メール利用を許可するか決めている"),
                ChecklistItem("contact_sns_dm", "SNS の DM 利用を許可するか決めている"),
                ChecklistItem(
                    "contact_game_chat",
                    "ゲーム内チャットを許可するか決めている",
                ),
            ),
        ),
        ChecklistCategory(
            id = "location",
            title = "位置・外出対策",
            description = "外出や予定外移動への備えを確認します。",
            items = listOf(
                ChecklistItem("location_watch", "位置情報見守りを使っている"),
                ChecklistItem(
                    "location_alerts",
                    "予定外移動時に通知される設定になっている",
                ),
                ChecklistItem(
                    "location_checkin",
                    "外出時のチェックインルールを決めている",
                ),
                ChecklistItem(
                    "location_solo",
                    "家族旅行や外出中の単独行動ルールを決めている",
                ),
            ),
        ),
    )

    /** 進捗集計用の総項目数。 */
    val totalItems: Int = categories.sumOf { it.items.size }

    /** 既知の全 id を返す (UI 進捗計算で未知 id を弾くため)。 */
    val knownItemIds: Set<String> =
        categories.flatMap { c -> c.items.map { it.id } }.toSet()
}
