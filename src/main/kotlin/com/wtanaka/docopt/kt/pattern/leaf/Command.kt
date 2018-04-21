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

internal class Command : Argument {
    constructor(name: String?) : super(name, Value.FALSE)
    constructor(name: String?, value: Value? = Value.FALSE) : super(name,
        value)

    override fun withValue(newValue: Value?): LeafPattern =
        Command(name, newValue)

    override fun singleMatch(
        left: List<LeafPattern>
    ): Pair<Int?, LeafPattern?> {
        val mainSeq = left
            .asSequence()
            .withIndex()
            .filter { it.value is Argument }
            .take(1)
            .filter {
                val value = it.value.value
                value is Value.AString && value.s == name
            }
            .map { (n, _) ->
                Pair(n, Command(name, Value.TRUE))
            }
        return (mainSeq + sequenceOf(Pair(null, null))).first()
    }
}
