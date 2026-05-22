package com.turtlesafety.radar.parent.format

import com.turtlesafety.radar.core.risk.DetectionSource
import com.turtlesafety.radar.core.risk.RiskCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class RiskLabelsTest {

    @Test
    fun levelLabel_returns_japanese_label_for_each_score() {
        assertEquals("問題なし", RiskLabels.levelLabel(0))
        assertEquals("注意", RiskLabels.levelLabel(1))
        assertEquals("要確認", RiskLabels.levelLabel(2))
        assertEquals("高め", RiskLabels.levelLabel(3))
        assertEquals("高リスク", RiskLabels.levelLabel(4))
        assertEquals("即時対応", RiskLabels.levelLabel(5))
    }

    @Test
    fun levelLabel_clamps_out_of_range_scores() {
        // 仕様では 0..5 だがエンジン異常時の防御的挙動を確認。
        assertEquals("問題なし", RiskLabels.levelLabel(-1))
        assertEquals("即時対応", RiskLabels.levelLabel(99))
    }

    @Test
    fun categoryLabel_returns_spec_japanese_names() {
        assertEquals("連絡先交換", RiskLabels.categoryLabel(RiskCategory.CONTACT_EXCHANGE))
        assertEquals("秘密化", RiskLabels.categoryLabel(RiskCategory.SECRECY))
        assertEquals("会う約束", RiskLabels.categoryLabel(RiskCategory.MEETUP))
        assertEquals("性的・自撮り要求", RiskLabels.categoryLabel(RiskCategory.SEXUAL_REQUEST))
        assertEquals("脅し・支配", RiskLabels.categoryLabel(RiskCategory.COERCION))
        assertEquals("身元不明", RiskLabels.categoryLabel(RiskCategory.IDENTITY_UNKNOWN))
    }

    @Test
    fun categoryLabels_handles_comma_joined_enum_names() {
        val joined = "MEETUP,SECRECY"
        assertEquals("会う約束 / 秘密化", RiskLabels.categoryLabels(joined))
    }

    @Test
    fun categoryLabels_ignores_unknown_enum_names_silently() {
        // 将来カテゴリ追加時にも UI が壊れないよう、不明 enum は無視する。
        val joined = "MEETUP,UNKNOWN_BUCKET,SECRECY"
        assertEquals("会う約束 / 秘密化", RiskLabels.categoryLabels(joined))
    }

    @Test
    fun categoryLabels_handles_empty_and_blank_input() {
        assertEquals("", RiskLabels.categoryLabels(""))
        assertEquals("", RiskLabels.categoryLabels("   "))
    }

    @Test
    fun sourceLabel_returns_japanese_for_each_enum() {
        assertEquals("通知", RiskLabels.sourceLabel(DetectionSource.NOTIFICATION))
        assertEquals("入力検査", RiskLabels.sourceLabel(DetectionSource.IME))
        assertEquals("画像", RiskLabels.sourceLabel(DetectionSource.IMAGE))
        assertEquals("QR コード", RiskLabels.sourceLabel(DetectionSource.QR))
        assertEquals("システム", RiskLabels.sourceLabel(DetectionSource.SYSTEM))
    }

    @Test
    fun sourceLabel_from_raw_string_falls_back_to_input_when_unknown() {
        // SYSTEM 等の既存値はそのまま使えるが、未知の DB 値が来ても落ちないこと。
        assertEquals("通知", RiskLabels.sourceLabel("NOTIFICATION"))
        assertEquals("UNKNOWN_VALUE", RiskLabels.sourceLabel("UNKNOWN_VALUE"))
    }
}
