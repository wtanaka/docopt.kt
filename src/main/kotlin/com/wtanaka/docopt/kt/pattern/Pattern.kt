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
package com.wtanaka.docopt.kt.pattern

import com.wtanaka.docopt.kt.OptionsShortcut
import com.wtanaka.docopt.kt.pattern.branch.BranchPattern
import com.wtanaka.docopt.kt.pattern.branch.Either
import com.wtanaka.docopt.kt.pattern.branch.OneOrMore
import com.wtanaka.docopt.kt.pattern.branch.Optional
import com.wtanaka.docopt.kt.pattern.branch.Required
import com.wtanaka.docopt.kt.pattern.leaf.Argument
import com.wtanaka.docopt.kt.pattern.leaf.Command
import com.wtanaka.docopt.kt.pattern.leaf.LeafPattern
import com.wtanaka.docopt.kt.pattern.leaf.Option

/**
 *
 */
@SuppressWarnings("TooManyFunctions")
internal abstract class Pattern {
    override fun equals(other: Any?): Boolean =
        other is Pattern && this.toString() == other.toString()

    override fun hashCode(): Int = this.toString().hashCode()
    /**
     * Port of fix from python docopt.
     */
    fun fix(): Pattern = fixIdentities().fixRepeatingArguments()

    internal abstract fun children(): List<Pattern>?
    internal abstract fun flat(types: List<Class<out LeafPattern>>):
        List<LeafPattern>

    /**
     * Apply the given transformer to every matched sub-node and return the
     * resulting tree.
     */
    internal abstract fun map(transformer: (Pattern) -> Pattern): Pattern

    internal abstract fun flat(): List<LeafPattern>

    /**
     * Make pattern-tree tips point to same object if they are equal.
     */
    internal abstract fun fixIdentities(uniq: List<LeafPattern>? = null):
        Pattern

    internal companion object {
        /**
         * Expand pattern into an (almost) equivalent one,
         * but with single Either.
         * Example: ((-a | -b) (-c | -d)) => (-a -c | -a -d | -b -c | -b -d)
         * Quirks: [-a] => (-a), (-a...) => (-a -a)
         */
        @SuppressWarnings("NestedBlockDepth")
        fun transform(pattern: Pattern): Either {
            var result = listOf<List<Pattern>>()
            var groups = listOf(listOf(pattern))
            val parents: List<Class<out Pattern>> =
                listOf<Class<out Pattern>>(
                    Required::class.java,
                    Optional::class.java,
                    OptionsShortcut::class.java,
                    Either::class.java,
                    OneOrMore::class.java)
            while (!groups.isEmpty()) {
                var children = groups.first()
                groups = groups.drop(1)
                val childrenTypes = children.map {
                    it::class.java
                }
                val parentMatches = parents.map(childrenTypes::contains)
                if (parentMatches.any { it }) {
                    val child = children.filter {
                        parents.contains(it::class.java)
                    }[0]
                    children = children.minus(child)
                    when (child) {
                        is Either -> child.children()?.forEach {
                            val newItem = listOf(it) + children
                            groups += listOf(newItem)
                        }
                        is OneOrMore -> {
                            val grandchildren = child.children() ?: listOf()
                            groups += listOf(grandchildren + grandchildren +
                                children)
                        }
                        else -> {
                            val grandchildren = child.children() ?: listOf()
                            groups += listOf(grandchildren +
                                children)
                        }
                    }
                } else {
                    result += listOf(children)
                }
            }
            return Either(result.map(::Required))
        }
    }

    private fun applyReplacements(
        replacements: Map<Pattern, Pattern>
    ): Pattern = replacements.getOrDefault(this, when (this) {
        is BranchPattern -> withChildren(
            children()?.map {
                it.applyReplacements(replacements)
            } ?: listOf())
        else -> this
    })

    /**
     * Fix elements that should accumulate/increment values.
     */
    internal fun fixRepeatingArguments(): Pattern {
        // flatten tree into an Either containing a flat list
        val either = transform(this)
        val grandkids = either
            // Get the list of LeafPattern
            .children()
            // Get the children of each Either
            ?.map {
                it.children() ?: listOf()
            }
        val replacements: Map<Pattern, Pattern> = grandkids
            // Traverse the Either's children and calculate replacements
            ?.map { childsKids ->
                childsKids.filter { grandchild ->
                    // Only consider duplicates
                    childsKids.count { it == grandchild } > 1
                }.filter {
                    it is Argument || it is Option || it is Command
                }.map { e: Pattern ->
                    @SuppressWarnings("UnsafeCast")
                    val replacement: Pattern = when {
                        e is Argument || e is Option && e.argcount > 0 -> when {
                            (e as LeafPattern).value == null -> e.withValue(
                                Value.AList(listOf()))
                            e.value is Value.AString -> {
                                val regex = Regex("\\s+")
                                val value: Value.AString = e.value
                                val s = value.s
                                val split = s.split(regex)
                                e.withValue(Value.AList(split))
                            }
                            else -> throw IllegalArgumentException("$e")
                        }
                        e is Command || e is Option && e.argcount == 0 ->
                            (e as LeafPattern).withValue(Value.AnInt(0))
                        else -> throw IllegalArgumentException("$e")
                    }
                    val entry: Pair<Pattern, Pattern> = e to replacement
                    entry
                }
            }
            // Convert into a map
            ?.flatten()?.toMap() ?: mapOf()
        return applyReplacements(replacements)
    }

    /**
     * Port of match from python docopt.
     */
    abstract fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>? = null
    ): PatternMatchResult
}
