package com.turtlesafety.radar.guard

/**
 * 仕様書 v0.4 §6 のチェックリスト項目を定義したカタログ。
 *
 * ID は将来 UI 順を変えても永続データが壊れないよう、安定したキー文字列を割り当てる。
 * 本アプリの責務は「保護者が外部制限を運用しているか確認する」ことのみ。
 * 個別サービスとの直接連携は行わない (§5.7)。
 */
object ChecklistDefinitions {

    val items: List<ChecklistItem> = buildList {
        // §6.1 アプリ制限
        category(ChecklistCategory.APP_RESTRICTIONS) {
            item("app.line", "LINE を許可するか整理しているか")
            item("app.openchat", "LINE オープンチャットを許可するか決めているか")
            item("app.browser", "ブラウザの利用方針を決めているか")
            item("app.sns", "Instagram / X / TikTok / Discord の許可方針を決めているか")
            item("app.gamechat", "ゲーム内チャットを許可するか決めているか")
            item("app.play_store", "Play ストアの利用に制限をかけているか")
            item("app.unknown_sources", "不明なアプリのインストールが OFF になっているか")
        }

        // §6.2 設定変更対策
        category(ChecklistCategory.SETTINGS_LOCKDOWN) {
            item("set.settings_app", "設定アプリへの自由アクセスを制限しているか")
            item("set.notif_listener_lock", "通知監視権限を子どもが解除できない運用になっているか")
            item("set.ime_switch", "IME 切り替えを簡単にできないようにしているか")
            item("set.vpn_proxy", "VPN やプロキシを勝手に追加できないようにしているか")
            item("set.app_install", "新規アプリインストールを制限しているか")
        }

        // §6.3 連絡手段対策
        category(ChecklistCategory.COMMUNICATION) {
            item("com.calls", "通話先制限を設定しているか")
            item("com.sms", "SMS 利用の許可方針を決めているか")
            item("com.email", "メール利用の許可方針を決めているか")
            item("com.sns_dm", "SNS の DM を許可するか決めているか")
            item("com.game_chat", "ゲーム内チャットの利用方針を決めているか")
        }

        // §6.4 位置・外出対策
        category(ChecklistCategory.LOCATION_AND_OUTING) {
            item("loc.tracking", "位置情報見守りを使っているか")
            item("loc.unexpected_move", "予定外移動時に通知される仕組みがあるか")
            item("loc.checkin_rule", "外出時のチェックインルールを決めているか")
            item("loc.solo_outing", "家族旅行中・外出中の単独行動ルールを決めているか")
        }
    }

    val byCategory: Map<ChecklistCategory, List<ChecklistItem>> by lazy {
        items.groupBy { it.category }
    }

    fun byId(id: String): ChecklistItem? = items.firstOrNull { it.id == id }

    // -------------------------------------------------------------------
    // small DSL helpers
    // -------------------------------------------------------------------

    private class CategoryBuilder(val category: ChecklistCategory) {
        val items = mutableListOf<ChecklistItem>()
        fun item(id: String, title: String, hint: String? = null) {
            items += ChecklistItem(id = id, category = category, title = title, hint = hint)
        }
    }

    private fun MutableList<ChecklistItem>.category(
        category: ChecklistCategory,
        block: CategoryBuilder.() -> Unit,
    ) {
        val builder = CategoryBuilder(category)
        builder.block()
        addAll(builder.items)
    }
}
