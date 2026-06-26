package com.github.nols1000.nutaru.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

@Suppress("UNUSED_PARAMETER")
internal actual fun openTestDriver(absolutePath: String, key: ByteArray): SqlDriver {
    // The JDBC sqlite-driver does not link SQLCipher; the key is accepted so
    // the same call shape as production is exercised, but encryption-at-rest
    // is verified by a separate filesystem-inspection check, not this test.
    return JdbcSqliteDriver(
        url = "jdbc:sqlite:$absolutePath",
        schema = NutaruDatabase.Schema,
    )
}

internal actual fun testDbAbsolutePath(name: String): String =
    File(System.getProperty("java.io.tmpdir", "."), name).absolutePath

internal actual fun deleteTestDb(absolutePath: String) {
    File(absolutePath).delete()
    // SQLite may also create -wal and -shm sidecars; remove them too.
    File("$absolutePath-wal").delete()
    File("$absolutePath-shm").delete()
}
