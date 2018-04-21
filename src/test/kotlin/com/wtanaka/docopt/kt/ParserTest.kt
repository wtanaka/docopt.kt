/*
 * docopt for Kotlin
 *
 * Original python reference implementation
 * Copyright (c) 2013 Vladimir Keleshev, vladimir@keleshev.com
 * Kotlin port
 * Copyright (c) 2018 Wesley Tanaka <https://wtanaka.com/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.wtanaka.docopt.kt

import com.wtanaka.docopt.kt.pattern.Value
import com.wtanaka.docopt.kt.pattern.branch.Either
import com.wtanaka.docopt.kt.pattern.branch.Optional
import com.wtanaka.docopt.kt.pattern.branch.Required
import com.wtanaka.docopt.kt.pattern.leaf.Command
import com.wtanaka.docopt.kt.pattern.leaf.Option
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

const val USAGE_OPTIONAL = "Usage: prog [add]"
const val USAGE_EITHER = "Usage: prog (add|rm)"
const val USAGE_AB = "Usage: prog a b"

class ParserTest {
    @Test
    fun testParseShort() {
        assertEquals(listOf(Option("-h", null, 0, Value.TRUE)),
            Parser("UNITTEST").parseShorts(Tokens(listOf("-h")),
                listOf()).result)
    }

    @Test
    fun testParseAtom() {
        assertEquals(listOf(Required(Optional(Command("add", Value.FALSE)))),
            Parser(USAGE_OPTIONAL).parseAtom(Tokens(listOf("(", "[", "add",
                "]", ")")), listOf()).result)
        assertEquals(listOf(Required(Either(listOf(
            Command("add", Value.FALSE),
            Command("rm", Value.FALSE))))),
            Parser(USAGE_EITHER).parseAtom(Tokens(listOf("(", "add",
                "|", "rm", ")", ")")), listOf()).result)
        Parser(USAGE_EITHER).parseAtom(Tokens(listOf(
            "(", "add", "|", "rm", ")")), listOf())
    }

    @Test
    fun testParseExpr() {
        assertEquals(
            listOf(Either(listOf(Command("add", Value.FALSE),
                Command("rm", Value.FALSE)))),
            Parser("UNIT TEST").parseExpr(Tokens(listOf("add", "|", "rm")),
                listOf()).result
        )
        assertEquals(
            Tokens(listOf(")", ")")),
            Parser("UNIT TEST").parseExpr(Tokens(listOf("rm", ")", ")")),
                listOf()).tokens)
        assertEquals(listOf(Optional(Option("-h", null, 0, Value.FALSE))),
            Parser("UNITTEST").parseExpr(Tokens.fromPattern("[ -h ]"),
                listOf()).result)
    }

    @Test
    fun testParseSeq() {
        assertEquals(listOf(Command("add")),
            Parser("TEST CODE").parseSeq(Tokens(listOf("add", ")")),
                listOf()).result)
        assertEquals(listOf(Required(Optional(Command("add", Value.FALSE)))),
            Parser(USAGE_OPTIONAL).parseSeq(Tokens(listOf("(", "[", "add",
                "]", ")")), listOf()).result)
    }

    @Test
    fun testParseDefaults() {
        assertEquals(listOf<Option>(), parseDefaults("Usage: prog [add]"))
        assertEquals(listOf<Option>(),
            parseDefaults("usage: prog [-ab]"))
    }

    @Test
    fun testParsePatternWithOptional() {
        val options = listOf<Option>()
        assertEquals(
            Required(Required(Optional(Command("add", Value.FALSE)))),
            Parser(USAGE_OPTIONAL).parsePattern(formalUsage(USAGE_OPTIONAL),
                options)
        )
    }

    @Test
    fun testParsePatternWithAB() {
        assertEquals(
            Required(Required(listOf(Command("a", Value.FALSE),
                Command("b", Value.FALSE)))),
            Parser(USAGE_AB).parsePattern(formalUsage(USAGE_AB),
                listOf())
        )
    }

    @Test
    fun testIsUpper() {
        assertFalse(Parser.isUpper("-"))
        assertFalse(Parser.isUpper("--"))
        assertFalse(Parser.isUpper("AbC"))
        assertFalse(Parser.isUpper("-b"))
        assertFalse(Parser.isUpper("abc"))
        assertTrue(Parser.isUpper("-B"))
        assertTrue(Parser.isUpper("A"))
        assertTrue(Parser.isUpper("ABC"))
    }

    @Test
    fun testParseArgv() {
        val usage = "usage: prog [-ab]"
        val parser = Parser(usage)
        val argv = parser.parseArgv(Tokens(listOf("-a")),
            parseDefaults(usage))
        assertEquals(listOf(Option("-a", null, 0, Value.TRUE)), argv.result)
    }
}