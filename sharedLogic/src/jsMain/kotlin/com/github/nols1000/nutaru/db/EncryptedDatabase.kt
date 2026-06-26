package com.github.nols1000.nutaru.db

import app.cash.sqldelight.db.SqlDriver

// JS is not shipped for V1 (see PRD Out of Scope). The expect/actual exists so
// the KMP graph compiles; calling this from JS throws.
internal actual fun createSqlCipherDriver(path: String, key: ByteArray): SqlDriver {
    throw UnsupportedOperationException(
        "Encrypted SQLite is not available on JS for V1 (PRD: webApp not shipped).",
    )
}
