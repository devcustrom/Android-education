package ru.devcustrom.androidlab.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * База данных приложения.
 *
 * `AppDatabase` — синглтон. Это важнее, чем кажется: если создать базу в двух
 * местах, будут два соединения к одному файлу, и Room честно об этом
 * предупредит. Синхронизация на `INSTANCE` сделана через `synchronized`, а
 * `@Volatile` гарантирует, что второе чтение поля увидит значение, записанное
 * первым потоком.
 *
 * Схема экспортируется в `android/app/schemas` — файл `1.json` нужен, чтобы
 * при следующей версии базы проверить миграцию, а не надеяться на глаз.
 *
 * ```
 *  version = 1  →  +1 поле у Note
 *                    ↓
 *  Room не умеет менять схему сам: нужен Migration или
 *  destructiveMigration. «Удалить и заполнить заново» — это
 *  migrate-with-destruct: у пользователя пропадут заметки.
 * ```
 */
@Database(
    entities = [Note::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        private const val DB_NAME = "notes.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                )
                    // Миграций пока нет: при смене схемы Room пересоздаст файл.
                    // Как только схема перестанет меняться — удалить.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}