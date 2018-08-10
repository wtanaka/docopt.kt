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

/**
 * port of Argument.
 */
internal open class Argument : LeafPattern {
    override fun withValue(newValue: Value?): LeafPattern =
        Argument(name, newValue)

    override fun singleMatch(left: List<LeafPattern>): Pair<Int?,
        LeafPattern?> {
        val mainSeq = left
            .withIndex()
            .filter { it.value is Argument }
            .map { (n, pattern) -> n to Argument(name, pattern.value) }
            .asSequence()
        val defaultReturn: Sequence<Pair<Int?, LeafPattern?>> =
            sequenceOf(null to null)
        return (mainSeq + defaultReturn).first()
    }

    internal constructor(name: String?) : super(name)
    internal constructor(name: String?, value: Value?) : super(name, value)
}
