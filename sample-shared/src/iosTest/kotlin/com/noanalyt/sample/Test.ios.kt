package com.noanalyt.sample

import kotlin.test.Test
import kotlin.test.assertTrue

class IosGreetingTest {

    @Test
    fun testExample() {
        assertTrue(getPlatform().name.contains("iOS"), "Check iOS is mentioned")
    }
}