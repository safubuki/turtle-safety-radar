package com.turtlesafety.radar.core.risk

/** 検知元 (仕様書 v0.4 §11.1)。 */
enum class DetectionSource {
    /** Notification Monitor 経由の通知本文。 */
    NOTIFICATION,

    /** Safety IME での入力前検査。 */
    IME,

    /** Media Checker の画像検査。 */
    IMAGE,

    /** QR コード検査。 */
    QR,

    /** 権限解除や IME 無効化などのシステム状態変化。 */
    SYSTEM,
}
