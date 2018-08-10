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
package com.wtanaka.docopt.kt.pattern.leaf

import com.wtanaka.docopt.kt.pattern.Value
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

/**
 * Represents a command line option.
 */
@SuppressFBWarnings("")
internal class Option(
    internal val short: String? = null,
    internal val long: String? = null,
    internal val argcount: Int = 0,
    value: Value? = Value.FALSE
) : LeafPattern(long ?: short,
    if (value == Value.FALSE && argcount > 0) null else value
) {
    init {
        assert(setOf(0, 1).contains(argcount))
    }

    override fun withValue(newValue: Value?): LeafPattern =
        Option(short, long, argcount, newValue)

    override fun singleMatch(left: List<LeafPattern>): Pair<Int?,
        LeafPattern?> = (left.mapIndexed { n: Int?, pattern: LeafPattern? ->
        // Pair<Int?, LeafPattern?>(n, pattern)
        n to pattern
    }.filter {
        name == it.second?.name
    } + listOf(
        // Pair<Int?, LeafPattern?>(null, null)
        null to null
    )).first()

    internal companion object {
        /**
         * Factory that constructs an Option from a String.
         */
        fun parse(optionDescription: String): Option {
            var short: String? = null
            var long: String? = null
            var argcount = 0
            var value: Value? = Value.FALSE
            val list = optionDescription.trim().split(Regex("  "), limit = 2)
            val options = list[0]
            val description = list.getOrElse(1, { "" })
            options
                .replace(',', ' ')
                .replace('=', ' ')
                .split(Regex("\\s+"))
                .forEach { s ->
                    when {
                        s.startsWith("--") -> long = s
                        s.startsWith("-") -> short = s
                        else -> argcount = 1
                    }
                }

            if (argcount > 0) {
                val matched = Regex("""\[default: (.*)\]""", RegexOption
                    .IGNORE_CASE).findAll(description)
                value = matched.firstOrNull()?.groupValues?.get(1)?.let {
                    Value.AString(it)
                }
            }

            return Option(short, long, argcount, value)
        }
    }

    /**
     * The long name if available, otherwise the short one.
     */
    public override val name: String?
        get() = long ?: short

    override fun toString(): String =
        "Option($short, $long, $argcount, $value)"
}
