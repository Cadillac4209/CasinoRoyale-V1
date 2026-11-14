package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ActiveGameStateDao
import com.example.data.local.dao.CurrencyDao
import com.example.data.local.entity.ActiveGameStateEntity
import com.example.data.local.entity.CurrencyTransactionEntity
import com.example.data.local.entity.PlayerCurrencyEntity

@Database(
    entities = [
        PlayerCurrencyEntity::class,
        CurrencyTransactionEntity::class,
        ActiveGameStateEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CasinoDatabase : RoomDatabase() {
    abstract fun currencyDao(): CurrencyDao
    abstract fun activeGameStateDao(): ActiveGameStateDao

    companion object {
        @Volatile
        private var INSTANCE: CasinoDatabase? = null

        fun getInstance(context: Context): CasinoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CasinoDatabase::class.java,
                    "royale_casino.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
