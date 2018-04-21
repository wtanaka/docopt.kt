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
 * ported from docopt.py.
 */
internal class Either : BranchPattern {
    override fun withChildren(newChildren: List<Pattern>) =
        Either(newChildren)

    constructor(children: List<Pattern>) : super(children)
    constructor(child: Pattern) : super(listOf(child))
    constructor(child1: Pattern, child2: Pattern) : super(listOf(child1,
        child2))

    constructor(p0: Pattern, p1: Pattern, p2: Pattern) : super(listOf(p0, p1,
        p2))

    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>?
    ): PatternMatchResult = with(collected ?: listOf()) {
        (children() ?: listOf()).map {
            it.match(left, this)
        }.filter(PatternMatchResult::matched).minBy {
            it.left.size
        } ?: PatternMatchResult(false, left, this)
    }
}
