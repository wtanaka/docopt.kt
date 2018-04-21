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

import com.wtanaka.docopt.kt.pattern.Pattern
import com.wtanaka.docopt.kt.pattern.PatternMatchResult
import com.wtanaka.docopt.kt.pattern.Value

/**
 * Leaf/terminal node of a pattern tree.
 */
internal abstract class LeafPattern constructor(
    internal open val name: String?,
    internal val value: Value? = null
) : Pattern() {
    /**
     * Port from python docopt.
     */
    abstract fun singleMatch(left: List<LeafPattern>): Pair<Int?, LeafPattern?>

    /**
     * Return this LeafPattern with the value replaced.
     */
    abstract fun withValue(newValue: Value?): LeafPattern

    @SuppressWarnings("ComplexMethod", "ReturnCount")
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>?
    ): PatternMatchResult {
        var myCollected = collected ?: listOf()
        val (pos, match) = singleMatch(left)
        if (match == null || pos == null) {
            return PatternMatchResult(false, left,
                myCollected)
        }
        val tmpLeft = left.subList(0, pos) + left.subList(pos + 1, left.size)
        val indexOfSameName = myCollected.indexOfFirst { it.name == name }
        if (value is Value.AnInt || value is Value.AList) {
            val increment = when {
                value is Value.AnInt -> Value.AnInt(1)
                match.value is Value.AString -> Value.AList(
                    listOf(match.value.s))
                else -> match.value ?: Value.AList(listOf())
            }
            if (indexOfSameName == -1) {
                return PatternMatchResult(true, tmpLeft,
                    myCollected + listOf(match.withValue(increment)))
            }
            val first = myCollected[indexOfSameName]
            @SuppressWarnings("UnsafeCast")
            myCollected = myCollected.subList(0, indexOfSameName) +
                listOf(first.withValue(when (first.value) {
                    is Value.AnInt -> first.value.plus(increment as Value.AnInt)
                    is Value.AList -> first.value.plus(increment as Value.AList)
                    else -> throw IllegalArgumentException("${first.value}")
                })) + myCollected.drop(indexOfSameName + 1)
            return PatternMatchResult(true, tmpLeft, myCollected)
        }
        return PatternMatchResult(true, tmpLeft, myCollected + listOf(match))
    }

    override fun children(): List<Pattern>? = null

    override fun flat(types: List<Class<out LeafPattern>>) =
        when {
            types.isEmpty() -> listOf(this)
            types.contains(this::class.java) -> listOf(this)
            else -> listOf()
        }

    /**
     * Make pattern-tree tips point to same object if they are equal.
     */
    override fun fixIdentities(uniq: List<LeafPattern>?): Pattern {
        val children = children()
        assert(children == null)
        return this
    }

    override fun map(transformer: (Pattern) -> Pattern): Pattern =
        transformer(this)

    override fun flat() = listOf(this)

    override fun toString() =
        "${this.javaClass.simpleName}($name, $value)"
}
