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

/**
 * Encapsulates a parsed value in a type-safe fashion.
 */
sealed class Value {
    /**
     * Encapsulates an int.
     */
    @SuppressWarnings("DataClassContainsFunctions")
    data class AnInt(private val i: Int) : Value() {
        /**
         * Combine this with another.
         */
        fun plus(other: AnInt) = Value.AnInt(i + other.i)
    }

    /**
     * Encapsulates a list.
     */
    @SuppressWarnings("DataClassContainsFunctions")
    data class AList(private val l: List<Any>) : Value() {
        /**
         * Combine this with another.
         */
        fun plus(other: AList) = Value.AList(l + other.l)
    }

    /**
     * Encapsulates a string.
     */
    @SuppressWarnings("DataClassContainsFunctions")
    data class AString(internal val s: String) : Value() {
        /**
         * Combine this with another.
         */
        fun plus(other: AString) = Value.AString(s + other.s)
    }

    /**
     * Represents a boolean.
     */
    abstract class Bool : Value()

    /**
     * true.
     */
    object TRUE : Value.Bool() {
        override fun toString(): String = "TRUE"
    }

    /**
     * false.
     */
    object FALSE : Bool() {
        override fun toString(): String = "FALSE"
    }
}
