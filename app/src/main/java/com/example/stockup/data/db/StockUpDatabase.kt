package com.example.stockup.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Product::class,
        ShoppingItem::class,
        ConsumptionHistory::class,
        PantryLocation::class,
        ConsumoObservacao::class,
        EstoqueAtualizacao::class,
        AvisoConsumo::class
    ],
    version = 5,
    exportSchema = false
)
abstract class StockUpDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun historyDao(): HistoryDao
    abstract fun locationDao(): LocationDao
    abstract fun consumoDao(): ConsumoDao

    companion object {
        @Volatile
        private var INSTANCE: StockUpDatabase? = null

        /**
         * 4 → 5: chegam as tabelas do motor de consumo.
         *
         * Só CREATE TABLE: nenhuma tabela antiga é tocada, nenhuma coluna muda.
         * É migração de acréscimo — quem já tem despensa continua com ela.
         *
         * Os nomes de coluna e os tipos têm de bater com o que o Room espera,
         * senão ele recusa abrir o banco ("Migration didn't properly handle").
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `consumo_observacoes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`chave` TEXT NOT NULL, " +
                        "`quantidade` REAL NOT NULL, " +
                        "`dias` REAL NOT NULL, " +
                        "`quando` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_consumo_observacoes_chave` ON `consumo_observacoes` (`chave`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_consumo_observacoes_quando` ON `consumo_observacoes` (`quando`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `estoque_atualizacoes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`chave` TEXT NOT NULL, " +
                        "`quantidadeAnterior` REAL NOT NULL, " +
                        "`quantidadeAtual` REAL NOT NULL, " +
                        "`quando` INTEGER NOT NULL, " +
                        "`origem` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_estoque_atualizacoes_chave` ON `estoque_atualizacoes` (`chave`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_estoque_atualizacoes_quando` ON `estoque_atualizacoes` (`quando`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `avisos_consumo` (" +
                        "`chave` TEXT NOT NULL, " +
                        "`ultimoAviso` INTEGER NOT NULL, " +
                        "`ignorados` INTEGER NOT NULL, " +
                        "`ultimaResposta` TEXT NOT NULL, " +
                        "`proximoAviso` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`chave`))"
                )
            }
        }

        fun getInstance(context: Context): StockUpDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockUpDatabase::class.java,
                    "stockup_database"
                )
                // Migração de verdade. O fallbackToDestructiveMigration (que recriava o
                // banco) saiu: com ele, subir de versão apagava a despensa de vocês.
                .addMigrations(MIGRATION_4_5)
                .build().also { INSTANCE = it }
            }
    }
}
