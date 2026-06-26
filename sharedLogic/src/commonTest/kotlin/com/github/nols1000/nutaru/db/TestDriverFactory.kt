package com.github.nols1000.nutaru.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Test-only driver factory.
 *
 * Production code uses [EncryptedDatabase.open] (real SQLCipher). The
 * commonTest round-trip uses this seam so it can run on the JVM host without
 * the SQLCipher native libraries — the test proves the schema, queries, and
 * close/reopen flow work; at-rest encryption is verified separately by the
 * filesystem-inspection criterion.
 *
 * On JVM host this is a JDBC sqlite-driver. On iOS/JS the actual throws;
 * those source sets compile but do not currently execute the round-trip.
 */
internal expect fun openTestDriver(absolutePath: String, key: ByteArray): SqlDriver

/** Absolute path for a temp DB file. Lives in the platform temp dir. */
internal expect fun testDbAbsolutePath(name: String): String

/** Best-effort delete of a temp DB file. No-op if missing. */
internal expect fun deleteTestDb(absolutePath: String): Unit
