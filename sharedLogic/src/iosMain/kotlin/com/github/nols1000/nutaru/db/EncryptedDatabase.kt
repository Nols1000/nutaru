package com.github.nols1000.nutaru.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

// TODO: NativeSqliteDriver links against the OS SQLite, not SQLCipher. To get
// encryption at rest on iOS, swap in a SQLCipher-linked SQLite via a pod
// (net.zetetic:sqlcipher-ios) and pass the key as `key = ...` in
// DatabaseConfiguration. The PRD requires iOS at V1, but the foundation issue
// is exercised on JVM/Android via commonTest; iOS wiring lands in a follow-up.
internal actual fun createSqlCipherDriver(path: String, key: ByteArray): SqlDriver {
    @Suppress("UNUSED_PARAMETER")
    val unused = key
    return NativeSqliteDriver(NutaruDatabase.Schema, path)
}
