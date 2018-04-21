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
import com.wtanaka.docopt.kt.pattern.leaf.LeafPattern

/**
 * Branch/inner node of a pattern tree.
 */
internal abstract class BranchPattern(
    /**
     * list of children.
     */
    private val children: List<Pattern>
) : Pattern() {
    /**
     * Return a new BranchPattern instance with its children replaced with
     * newChildren.
     */
    abstract fun withChildren(newChildren: List<Pattern>): BranchPattern

    override fun children(): List<Pattern>? = children

    @SuppressWarnings("SpreadOperator")
    override fun flat(types: List<Class<out LeafPattern>>): List<LeafPattern> =
        this.children
            .map { it.flat(types) }
            .fold(listOf()) { a, b -> a + b }

    @SuppressWarnings("SpreadOperator")
    override fun map(transformer: (Pattern) -> Pattern): Pattern {
        val newChildren = this.children.map {
            val possibleNew = transformer(it)
            when (it !== possibleNew) {
                true -> possibleNew
                false -> it.map(transformer)
            }
        }
        return withChildren(newChildren)
    }

    override fun flat(): List<LeafPattern> =
        children.map(Pattern::flat).fold(listOf()) { a, b -> a + b }

    /**
     * Make pattern-tree tips point to same object if they are equal.
     */
    override fun fixIdentities(uniq: List<LeafPattern>?): Pattern {
        val children = children()
        if (children == null) {
            throw AssertionError()
        } else {
            val thisUniq = uniq ?: this.flat().toSet().toList()
            val newChildren = children.map { child ->
                when (child.children()) {
                    null -> {
                        assert(thisUniq.contains(child))
                        thisUniq[thisUniq.indexOf(child)]
                    }
                    else -> child.fixIdentities(thisUniq)
                }
            }
            return withChildren(newChildren)
        }
    }

    override fun toString() = this.javaClass.simpleName +
        "(${this.children.joinToString(", ",
            transform = Pattern::toString)})"
}
