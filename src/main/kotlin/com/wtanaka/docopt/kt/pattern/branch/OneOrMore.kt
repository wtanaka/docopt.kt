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
package com.wtanaka.docopt.kt.pattern.branch

import com.wtanaka.docopt.kt.pattern.Pattern
import com.wtanaka.docopt.kt.pattern.PatternMatchResult
import com.wtanaka.docopt.kt.pattern.leaf.LeafPattern

/**
 * ?.
 */
@SuppressWarnings("SpreadOperator")
internal class OneOrMore : BranchPattern {
    override fun withChildren(newChildren: List<Pattern>) =
        OneOrMore(newChildren)

    constructor(children: List<Pattern>) : super(children)
    constructor(child: Pattern) : super(listOf(child))
    constructor(p0: Pattern, p1: Pattern) : super(listOf(p0, p1))

    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>?
    ): PatternMatchResult {
        @SuppressWarnings("UnsafeCallOnNullableType")
        val children = this.children()!!
        assert(children.size == 1)
        val child0 = children[0]
        val myCollected = collected ?: listOf()
        var l = left
        var c = myCollected
        var lll: List<LeafPattern>? = null
        var matched = true
        var times = 0
        while (matched) {
            // could it be that something didn't match but changed l or c?
            val result = child0.match(l, c)
            matched = result.matched
            l = result.left
            c = result.collected
            times += if (matched) 1 else 0
            if (lll == l) {
                break
            }
            lll = l
        }
        if (times >= 1) {
            return PatternMatchResult(true, l, c)
        }
        return PatternMatchResult(false, left, myCollected)
    }
}
