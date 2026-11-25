package com.group8.wellbeing.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RawUsageEventEntity::class,
        CollectionBatchEntity::class,
        FeatureWindowEntity::class,
        PredictionEntity::class,
        ReminderEventEntity::class,
        FeedbackEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WellbeingDatabase : RoomDatabase() {
    abstract fun wellbeingDao(): WellbeingDao

    companion object {
        @Volatile
        private var instance: WellbeingDatabase? = null

        fun getInstance(context: Context): WellbeingDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WellbeingDatabase::class.java,
                    "wellbeing_local.db"
                ).build().also { created ->
                    instance = created
                }
            }
        }
    }
}
