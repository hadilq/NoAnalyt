package com.noanalyt.sample

import kotlin.test.Test
import kotlin.test.assertTrue

class CommonGreetingTest {

    @Test
    fun testExample() {
        assertTrue(getPlatform().name.contains("Android"), "Check iOS is mentioned")
    }
}