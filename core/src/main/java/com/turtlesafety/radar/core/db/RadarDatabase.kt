package com.turtlesafety.radar.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.turtlesafety.radar.core.log.DetectionLog
import com.turtlesafety.radar.core.log.DetectionLogDao

@Database(
    entities = [DetectionLog::class],
    version = 1,
    exportSchema = false,
)
abstract class RadarDatabase : RoomDatabase() {

    abstract fun detectionLogDao(): DetectionLogDao

    companion object {
        private const val DB_NAME = "radar.db"

        @Volatile private var instance: RadarDatabase? = null

        fun get(context: Context): RadarDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): RadarDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                RadarDatabase::class.java,
                DB_NAME,
            )
                // 開発初期の互換性破壊を許容。リリース版では明示的な Migration を追加する。
                .fallbackToDestructiveMigration()
                .build()
    }
}
