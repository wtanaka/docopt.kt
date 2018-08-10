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

import com.wtanaka.docopt.kt.pattern.Pattern
import com.wtanaka.docopt.kt.pattern.Value
import com.wtanaka.docopt.kt.pattern.branch.Either
import com.wtanaka.docopt.kt.pattern.branch.OneOrMore
import com.wtanaka.docopt.kt.pattern.branch.Optional
import com.wtanaka.docopt.kt.pattern.branch.Required
import com.wtanaka.docopt.kt.pattern.leaf.Argument
import com.wtanaka.docopt.kt.pattern.leaf.Command
import com.wtanaka.docopt.kt.pattern.leaf.LeafPattern
import com.wtanaka.docopt.kt.pattern.leaf.Option

@SuppressWarnings("LargeClass")
internal class Parser(private val usage: String) {
    // long ::= '--' chars [ ( ' ' | '=' ) chars ] ;.
    @SuppressWarnings("ComplexMethod", "ThrowsCount")
    private fun parseLong(
        origTokens: Tokens,
        options: List<Option>
    ): ParseState<LeafPattern> {
        var newOptions = options
        val pair = origTokens.move()
        val popped = pair.first
        var tokens = pair.second
        val split = popped?.split("=", limit = 2)
        val long = split?.get(0)
        assert(long?.startsWith("--") == true)
        var value = split?.getOrNull(1)?.let { Value.AString(it) }
        var similar = newOptions.filter { o -> o.long == long }
        // if no exact match
        if (long != null &&
            tokens.errorFactory == DocoptExit.FACTORY &&
            similar.isEmpty()) {
            similar = newOptions.filter { o ->
                o.long != null && o.long.startsWith(long)
            }
        }
        var o: Pattern?
        if (similar.size > 1) {
            // might be simply specified ambiguously 2+ times?
            throw tokens.error(usage,
                "$long is not a unique prefix: ${similar.map(
                    Option::long)
                    .joinToString(", ")}?")
        } else if (similar.isEmpty()) {
            val argcount = when (value) {
                null -> 0
                else -> 1
            }
            o = Option(null, long, argcount)
            newOptions += o
            if (tokens.errorFactory == DocoptExit.FACTORY) {
                o = Option(null, long,
                    argcount,
                    if (argcount > 0) value else Value.TRUE)
            }
        } else {
            o = Option(similar[0].short,
                similar[0].long, similar[0].argcount,
                similar[0].value)
            if (o.argcount == 0) {
                if (value != null) {
                    throw tokens.error(
                        usage,
                        "${o.long} must not have an argument")
                }
            } else {
                if (value == null) {
                    if (setOf(null, "--").contains(tokens.current())) {
                        throw tokens.error(
                            usage,
                            "${o.long} requires argument")
                    }
                    val moved = tokens.move()
                    value = moved.first?.let { Value.AString(it) }
                    tokens = moved.second
                }
            }
            if (tokens.errorFactory == DocoptExit.FACTORY) {
                o = Option(o.short, o.long,
                    o.argcount, value ?: Value.TRUE)
            }
        }
        return ParseState(listOf(o), tokens, listOf(o))
    }

    /**
     * shorts ::= '-' ( chars )* [ [ ' ' ] chars ] ;.
     */
    @SuppressWarnings("ComplexMethod", "NestedBlockDepth", "ThrowsCount")
    internal fun parseShorts(
        origTokens: Tokens,
        origOptions: List<Option>
    ): ParseState<LeafPattern> {
        var options = origOptions
        var (token, tokens) = origTokens.move()
        if (token == null) {
            throw AssertionError()
        } else {
            assert(token.startsWith("-") || !token.startsWith("--"))
        }
        var left = token.substring(1)
        var parsed = listOf<Option>()
        while (left != "") {
            val short = "-" + left[0]
            left = left.substring(1)
            val similar = options.filter { o -> o.short == short }
            val o: Option = when {
                similar.size > 1 -> throw tokens.error(usage,
                    "$short is specified ambiguously ${similar.size} times")
                similar.isEmpty() -> {
                    var o =
                        Option(short, null,
                            0)
                    options += o
                    if (tokens.errorFactory == DocoptExit.FACTORY) {
                        o = Option(short,
                            null, 0,
                            Value.TRUE)
                    }
                    o
                }
                else -> {
                    val o = Option(short,
                        similar[0].long, similar[0].argcount,
                        similar[0].value)
                    var value: Value? = null
                    if (o.argcount != 0) {
                        if (left == "") {
                            if (tokens.current() in setOf(null, "--")) {
                                throw tokens.error(usage,
                                    "$short requires argument")
                            }
                            val moved2 = tokens.move()
                            value = moved2.first?.let { Value.AString(it) }
                            tokens = moved2.second
                        } else {
                            value = Value.AString(left)
                            left = ""
                        }
                    }

                    when (tokens.errorFactory) {
                        DocoptExit.FACTORY -> Option(
                            o.short, o.long,
                            o.argcount, value ?: Value.TRUE)
                        else -> o
                    }
                }
            }
            parsed += o
        }
        return ParseState(parsed, tokens, options)
    }

    /**
     * ported from docopt.
     */
    internal fun parsePattern(source: String, options: List<Option>): Required {
        var tokens = Tokens.fromPattern(source)
        val result = parseExpr(tokens, options)
        tokens = result.tokens
        if (tokens.current() != null) {
            throw tokens.error(
                usage,
                "unexpected ending: ${tokens.joinToString(" ")}")
        }
        return Required(result.result)
    }

    /**
     * expr ::= seq ( '|' seq )* ;.
     */
    internal fun parseExpr(
        origTokens: Tokens,
        options: List<Option>
    ): ParseState<Pattern> {
        var parseState = parseSeq(origTokens, options)
        var tokens = parseState.tokens
        if (tokens.current() != "|") {
            return parseState
        }
        var result = when {
            parseState.result.size > 1 -> listOf(Required(parseState.result))
            else -> parseState.result
        }

        while (tokens.current() == "|") {
            val firstLast = tokens.move()
            tokens = firstLast.second
            parseState = parseSeq(tokens, options)
            tokens = parseState.tokens
            result += when {
                parseState.result.size > 1 -> listOf(
                    Required(parseState.result))
                else -> parseState.result
            }
        }

        return ParseState(
            if (result.size > 1) listOf(Either(result)) else result,
            tokens, options)
    }

    /**
     * seq ::= ( atom [ '...' ] )* ;.
     */
    internal fun parseSeq(
        origTokens: Tokens,
        options: List<Option>
    ): ParseState<Pattern> {
        var tokens: Tokens = origTokens
        var atom: List<Pattern>
        var result = listOf<Pattern>()
        while (!setOf(null, "]", ")", "|").contains(tokens.current())) {
            val parseResult = parseAtom(tokens, options)
            tokens = parseResult.tokens
            atom = parseResult.result
            if (tokens.current() == "...") {
                atom = listOf(OneOrMore(atom))
                val x = tokens.move()
                tokens = x.second
            }
            result += atom
        }

        return ParseState(result, tokens, options)
    }

    /**
     * atom ::= '(' expr ')' | '[' expr ']' | 'options'
     *     | long | shorts | argument | command ;.
     */
    @SuppressWarnings("ComplexMethod", "ReturnCount")
    internal fun parseAtom(
        origTokens: Tokens,
        options: List<Option>
    ): ParseState<Pattern> {
        val token = origTokens.current()
        if (token == "(" || token == "[") {
            var (_, tokens) = origTokens.move()
            val parseExpr = parseExpr(tokens, options)
            val pair = when (token) {
                "(" -> ")" to Required(parseExpr.result)
                "[" -> "]" to Optional(parseExpr.result)
                else -> throw AssertionError()
            }
            tokens = parseExpr.tokens
            val split = tokens.move()
            tokens = split.second
            if (split.first != pair.first) {
                throw tokens.error(usage, "Unmatched '$token'")
            }
            val result = pair.second
            return ParseState(listOf(result), tokens, options)
        } else if (token == "options") {
            val (_, tokens) = origTokens.move()
            return ParseState(listOf(OptionsShortcut()), tokens, options)
        } else if (token?.startsWith("--") == true && token != "--") {
            return parseLong(origTokens, options)
        } else if (token?.startsWith("-") == true &&
            !setOf("-", "--").contains(token)) {
            return parseShorts(origTokens, options)
        } else if (token?.startsWith("<") == true && token.endsWith(
                ">") || isUpper(token)) {
            val x = origTokens.move()
            return ParseState(listOf(Argument(x.first)), x.second, options)
        } else {
            val x = origTokens.move()
            return ParseState(listOf(
                Command(x.first)), x.second, options)
        }
    }

    /**
     * Parse command-line argument vector.
     * If options_first:
     * argv ::= [ long | shorts ]* [ argument ]* [ '--' [ argument ]* ] ;
     * else:
     * argv ::= [ long | shorts | argument ]* [ '--' [ argument ]* ] ;
     * .
     */
    @SuppressWarnings("ComplexMethod", "ReturnCount")
    fun parseArgv(
        origTokens: Tokens,
        options: List<Option>,
        optionsFirst: Boolean = false
    ): ParseState<LeafPattern> {
        var tokens = origTokens
        var parsed: List<LeafPattern> = listOf()
        while (tokens.current() != null) {
            @SuppressWarnings("UnsafeCallOnNullableType")
            val current = tokens.current()!!
            if (current == "--") {
                return ParseState(parsed + tokens.map {
                    Argument(null, Value.AString(it))
                }, tokens, options)
            } else if (current.startsWith("--")) {
                val longParse = parseLong(tokens, options)
                tokens = longParse.tokens
                parsed += longParse.result
            } else if (current.startsWith("-") && current != "-") {
                val shortParse = parseShorts(tokens, options)
                tokens = shortParse.tokens
                parsed += shortParse.result
                assert(parsed.isNotEmpty())
            } else if (optionsFirst) {
                return ParseState(parsed + tokens.map {
                    Argument(null, Value.AString(it))
                }, tokens, options)
            } else {
                val moved = tokens.move()
                tokens = moved.second
                @SuppressWarnings("UnsafeCallOnNullableType")
                parsed += Argument(null,
                    Value.AString(moved.first!!))
            }
        }
        return ParseState(parsed, tokens, options)
    }

    internal companion object {
        internal fun isUpper(token: String?): Boolean =
            with(token?.toUpperCase()) {
                val hasLetters = this != this?.toLowerCase()
                val allUpper = this == token
                hasLetters && allUpper
            }
    }
}
