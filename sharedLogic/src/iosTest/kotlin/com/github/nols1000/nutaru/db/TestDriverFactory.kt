package com.github.nols1000.nutaru.db

import app.cash.sqldelight.db.SqlDriver

// The commonTest round-trip is JVM-only today. These actuals exist so the KMP
// graph compiles for iOS; they throw if reached.
internal actual fun openTestDriver(absolutePath: String, key: ByteArray): SqlDriver =
    throw UnsupportedOperationException("Round-trip test driver is not wired on iOS.")

internal actual fun testDbAbsolutePath(name: String): String =
    throw UnsupportedOperationException()

internal actual fun deleteTestDb(absolutePath: String) {}
