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

/**
 * list of tokens.
 */
class Tokens internal constructor(
    // The class of the error.
    private val source: List<String>,
    internal val errorFactory: ErrorFactory = DocoptExit.FACTORY
) {
    internal fun joinToString(s: String) = source.joinToString(s)

    internal fun error(usage: String, s: String): Throwable =
        errorFactory.create(usage, s)

    internal fun current(): String? = source.getOrNull(0)
    internal fun move() = source.getOrNull(0) to Tokens(
        source.drop(1), errorFactory)

    internal fun <T> map(lambda: (String) -> (T)) = source.map(lambda)
    internal fun toList() = source.toList()
    internal fun size() = source.size
    override fun toString() = source.toString()
    override fun hashCode(): Int = source.hashCode() xor errorFactory.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is Tokens -> source == other.source &&
            errorFactory == other.errorFactory
        else -> false
    }

    companion object {
        internal fun fromPattern(source: String): Tokens {
            val source1 = Regex(
                """([\[\]()|]|\.\.\.)""").replace(source, {
                " ${it.groupValues[1]} "
            })
            // XXX: The python reference implementation has a more complicated
            // regex and a more complicated implementation of split() which
            // differs from this.
            return Tokens(source1.split("""\s+""".toRegex())
                .filter(String::isNotEmpty), DocoptLanguageError.FACTORY)
        }
    }
}
