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

import com.wtanaka.docopt.kt.pattern.Pattern
import com.wtanaka.docopt.kt.pattern.Value
import com.wtanaka.docopt.kt.pattern.leaf.LeafPattern
import com.wtanaka.docopt.kt.pattern.leaf.Option

/**
 * "usage:" literal string.
 */
const val USAGE_SECTION = "usage:"

/**
 * Parse a sequence of options with their default values.
 */
internal fun parseDefaults(doc: String): List<Option> =
    parseSection("options:", doc).map {
        it
            // Get rid of "options:"
            .split(':', limit = 2)
            .getOrElse(1, { "" })
            .let { "\n" + it }
            .let { splitOptions(it) }
            .drop(1)
            .let {
                val lhs = it.windowed(1, 2).map { it[0] }
                val rhs = it.drop(1).windowed(1, 2).map { it[0] }
                lhs.zip(rhs)
            }.map {
                it.first + it.second
            }.filter {
                it.startsWith("-")
            }.map(Option.Companion::parse)
    }.flatten().toList()

internal fun splitOptions(s: String, isStart: Boolean = true) =
    reSplit(Regex("""\n[ \t]*(-\S+?)"""), s, isStart = isStart)

/**
 * Emulator of python re.split() for the splitOptions pattern.
 */
fun reSplit(
    regex: Regex,
    s: String,
    isStart: Boolean = true
): List<String> {
    val match = regex.find(s)
    return when (match) {
        null -> listOf(s)
        else -> {
            val first = when (match.range.first) {
            // if there are capturing groups in the separator and it matches
            // the start of the string
                0 -> if (isStart) listOf("") else listOf()
                else -> listOf(s.substring(0, match.range.first))
            }
            val second = listOf(match.groupValues[1])
            val third = when (match.range.last) {
                s.length - 1 -> listOf("")
                else -> splitOptions(
                    s.substring(match.range.last + 1), isStart = true)
            }
            first + second + third
        }
    }
}

/**
 * Extract contents of section.
 */
fun parseSection(name: String, source: String): Sequence<String> {
    val pattern: Regex = ("^([^\n]*" + name +
        "[^\n]*\n?(?:[ \t].*?(?:\n|$))*)").toRegex(
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
    return pattern.findAll(source).map { it.value.trim() }
}

/**
 * ported from docopt.
 */
fun formalUsage(section: String): String {
    val pu = section
        .split(":", limit = 2)
        .getOrElse(1, { "" })
        .trim()
        .split(Regex("\\s+"))
    return "( " + pu.drop(1).joinToString(" ") { s ->
        when (s) {
            pu[0] -> ") | ("
            else -> s
        }
    } + " )"
}

internal fun extras(
    help: Boolean,
    version: Any?,
    options: List<LeafPattern>,
    doc: String
) {
    if (help && options.map { it ->
            setOf("-h", "--help").contains(it.name) && it.value != null
        }.any { it }) {
        throw DocoptExit(0, doc)
    }
    if (version != null && options.map {
            it.name == "--version" && it.value != null
        }.any { it }) {
        throw DocoptExit(0, doc)
    }
}

/**
 * @param doc Description of your command-line interface.
 * @param argv Argument vector to be parsed.
 * @param help Set to False to disable automatic help on -h or --help options.
 * @param version If passed, the object will be printed if --version is in argv.
 * @param optionsFirst Set to True to require options precede positional
 * arguments, i.e. to forbid options and positional arguments intermix.
 *
 * @return A dictionary, where keys are names of command-line elements such
 * as e.g. "--verbose" and "<path>", and values are the parsed values of
 * those elements.
 */
@SuppressWarnings("ThrowsCount")
fun docopt(
    doc: String,
    argv: Array<String> = arrayOf(),
    help: Boolean = true,
    version: Any? = null,
    optionsFirst: Boolean = false
): Map<String?, Value?> {
    val usageSections = parseSection(USAGE_SECTION, doc).toList()
    when {
        usageSections.isEmpty() -> throw DocoptLanguageError(
            "\"$USAGE_SECTION\" (case-insensitive) not found.")
        usageSections.size > 1 -> throw DocoptLanguageError(
            "More than one \"$USAGE_SECTION\" (case-insensitive).")
    }
    val usage = usageSections[0]
    val parser = Parser(usage)
    val options = parseDefaults(doc)
    var pattern: Pattern = parser.parsePattern(formalUsage(usage), options)
    val parsedArgv = parser.parseArgv(Tokens(argv.toList()), options,
        optionsFirst)
    val patternOptions = pattern.flat(listOf(Option::class.java)).toSet()
    pattern = pattern.map {
        when (it) {
            is OptionsShortcut -> {
                val docOptions = parseDefaults(doc)
                it.withChildren((docOptions.toSet() - patternOptions).toList())
            }
            else -> it
        }
    }
    extras(help, version, parsedArgv.result, doc)
    pattern = pattern.fix()
    val match = pattern.match(parsedArgv.result)
    if (match.matched && match.left.isEmpty()) {
        return (pattern.flat() + match.collected).map {
            it.name to it.value
        }.toMap()
    }
    throw DocoptExit(1, "Internal error")
}

/**
 * Syntactical sugar for the Array<String> version of docopt().
 */
fun docopt(
    doc: String,
    argv: String,
    help: Boolean = true,
    version: Any? = null,
    optionsFirst: Boolean = false
): Map<String?, Value?> =
    docopt(doc, arrayOf(argv), help, version, optionsFirst)
