package com.noanalyt.sample

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class AndroidGreetingTest {

    @BeforeTest
    fun prepareRuntime() {
        com.noanalyt.runtime.configure(true)
    }

    @Test
    fun testExample() {
        assertTrue(getPlatform().name.contains("Android"), "Check Android is mentioned")
    }

    @Test
    fun testNoAnalytExample() {
        val logOutput = readSystemOut {
            assertTrue(getPlatform().name.contains("Android"), "Check logs are there")
        }
        println("log is: $logOutput")
        assertTrue(logOutput.contains("Android"), "Check Android is mentioned")
    }

    fun readSystemOut(runner: () -> Unit): String {
        val original = System.out
        val stream = ByteArrayOutputStream()
        val current = PrintStream(stream)
        System.setOut(current)
        // Run command
        runner()
        System.setOut(original)
        return stream.toString()
    }
}