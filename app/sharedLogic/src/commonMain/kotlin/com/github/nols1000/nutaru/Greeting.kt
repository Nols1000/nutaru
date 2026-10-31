package com.github.nols1000.nutaru

import kotlin.js.JsExport

@JsExport
class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}