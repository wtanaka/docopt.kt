package com.wtanaka.docopt.kt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TokensTest {
    @Test
    fun testFromPattern() {
        assertEquals(3, Tokens.fromPattern("( add )").size())
        assertEquals(listOf("(", "[", "add", "]", ")"),
            Tokens.fromPattern("( [add] )").toList())
        assertEquals(listOf("(", "(", "add", "|", "rm", ")", ")"),
            Tokens.fromPattern("( (add|rm) )").toList())
    }

    @Test
    fun testFromPatternErrorClass() {
        assertNotEquals(DocoptExit.FACTORY, Tokens.fromPattern("[ -h ]")
            .errorFactory)
    }
}