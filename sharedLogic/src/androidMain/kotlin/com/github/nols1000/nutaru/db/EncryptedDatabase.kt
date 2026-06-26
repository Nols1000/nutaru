package com.github.nols1000.nutaru.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Process-wide Application Context, set once from the Android entry point
 * (Application.onCreate or androidx.startup Initializer) before any DB call.
 *
 * The library never holds an Activity context. Stored as a static so commonMain
 * code can open the database without threading Context through every call.
 */
object AppContextHolder {
    @Volatile
    var context: Context? = null
}

private const val DEFAULT_DB_NAME = "nutaru.db"

internal actual fun createSqlCipherDriver(path: String, key: ByteArray): SqlDriver {
    val context = AppContextHolder.context
        ?: error("AppContextHolder.context must be set before opening the encrypted database.")
    // [path] is treated as the DB file name within the app's database directory,
    // matching AndroidSqliteDriver's expectations (it calls context.openOrCreateDatabase(name)).
    val name = path.ifBlank { DEFAULT_DB_NAME }
    val factory = SupportOpenHelperFactory(key)
    return AndroidSqliteDriver(
        schema = NutaruDatabase.Schema,
        context = context,
        name = name,
        factory = factory,
    )
}
