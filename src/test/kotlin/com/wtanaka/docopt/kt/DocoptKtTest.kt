package com.wtanaka.docopt.kt

import com.wtanaka.docopt.kt.pattern.Pattern
import com.wtanaka.docopt.kt.pattern.PatternMatchResult
import com.wtanaka.docopt.kt.pattern.Value
import com.wtanaka.docopt.kt.pattern.branch.Either
import com.wtanaka.docopt.kt.pattern.branch.OneOrMore
import com.wtanaka.docopt.kt.pattern.branch.Optional
import com.wtanaka.docopt.kt.pattern.branch.Required
import com.wtanaka.docopt.kt.pattern.leaf.Argument
import com.wtanaka.docopt.kt.pattern.leaf.Command
import com.wtanaka.docopt.kt.pattern.leaf.LeafPattern
import com.wtanaka.docopt.kt.pattern.leaf.Option
import org.junit.Assert
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.test.assertNotSame

fun <T : Throwable> assertRaises(clazz: Class<T>, lambda: () -> Unit) {
    try {
        lambda()
        Assert.fail("Expecting $clazz")
    } catch (e: Throwable) {
        if (!clazz.isInstance(e)) {
            throw e
        }
    }
}

internal class DocoptKtTest {
    @Test
    fun testFormalUsageExtras() {
        assertEquals("( add )", formalUsage("Usage: prog add"))
        assertEquals("( [add] )", formalUsage("Usage: prog [add]"))
        assertEquals("( (add|rm) )",
            formalUsage("Usage: prog (add|rm)"))
        assertEquals("( a b )",
            formalUsage("Usage: prog a b"))
        assertEquals("(  )",
            formalUsage("Usage: prog"))
        assertEquals("( [options] Options: --all All. )",
            formalUsage("""Usage: prog [options]
                |
                |Options: --all  All.
            """.trimMargin()))
        assertEquals("( [options] Options: -v, --verbose Verbose. )",
            formalUsage("""Usage: prog
            |[options]
            |
            |Options: -v, --verbose  Verbose.
        """.trimMargin()))
    }

    @Test
    fun testMatch() {
        val parser = Parser("unit test")
        val argv = parser.parseArgv(Tokens(listOf("add")), listOf(), false)
        assertEquals(
            PatternMatchResult(true, listOf(),
                listOf(Command("add", Value.TRUE))),
            Required(Required(Command("add", Value.TRUE))).fix().match(
                argv.result))
        assertEquals(PatternMatchResult(true, listOf(), listOf(Option("-a",
            null, 0, Value.TRUE))),
            Required(Required(Optional(Option("-a", null, 0, Value.FALSE),
                Option("-b", null, 0, Value.FALSE)))).match(
                listOf(Option("-a", null, 0, Value.TRUE))))
    }

    @Test
    fun testPatternFlat() {
        assertEquals(
            listOf(
                Argument("N"),
                Option("-a"),
                Argument("M")),
            Required(listOf(
                OneOrMore(Argument("N")),
                Option("-a"),
                Argument("M"))).flat())
        Assert.assertEquals(listOf(Option("-v", null, 0), Option("-v", null,
            0)), Required(Required(
            Optional(Option("-v", null, 0),
                Option("-v", null, 0)))).flat(listOf(Option::class.java)))
        Assert.assertEquals(setOf(Option("-v", null, 0)),
            Required(Required(
                Optional(Option("-v", null, 0),
                    Option("-v", null, 0)))).flat(
                listOf(Option::class.java)).toSet()
        )
    }

    @Test
    fun testSplitOptions() {
        assertEquals(listOf("", "--", "foo=<arg>  [default: bar]"),
            splitOptions("\n\t--foo=<arg>  [default: bar]"))
        assertEquals(listOf("", "--", "foo=<arg>  [default: bar]", "-h", ""),
            splitOptions("\n\t--foo=<arg>  [default: bar]\n\t-h"))
        assertEquals(listOf("\n ", "-h", "1234\n"),
            splitOptions("\n \n-h1234\n"))
        assertEquals(listOf("", "--", "foo=<arg>  [default: bar]", "--",
            "bar<arg>  [default: baz]"),
            splitOptions("\n\t--foo=<arg>  [default: bar]" +
                "\n\t--bar<arg>  [default: baz]"))
        assertEquals(listOf("\n", "--", "foo=<arg>  [default: bar]"),
            splitOptions("\n\n\t--foo=<arg>  [default: bar]"))
        assertEquals(listOf("", "--", "long  ARG"),
            splitOptions("\n --long  ARG"))
        assertEquals(listOf("", "--", "long  ARG"),
            splitOptions("\n--long  ARG"))
        assertEquals(listOf("", "--", "long ARG"),
            splitOptions("\n --long ARG"))
        assertEquals(listOf("", "--", "long ARG"),
            splitOptions("\n--long ARG"))
        assertEquals(listOf("", "-x", "", "-y", ""),
            splitOptions("\n" + " -x\n -y"))
    }

    @Test
    fun testCommands() {
        assertEquals(mapOf("add" to Value.TRUE),
            docopt("Usage: prog add", listOf("add").toTypedArray()))
        assertEquals(mapOf("add" to Value.FALSE), docopt("Usage: prog [add]",
            listOf<String>().toTypedArray()))
        assertEquals(mapOf("add" to Value.TRUE),
            docopt("Usage: prog [add]", listOf("add").toTypedArray()))
        assertEquals(mapOf("add" to Value.TRUE, "rm" to Value.FALSE),
            docopt("Usage: prog (add|rm)",
                listOf("add").toTypedArray()))
        assertEquals(mapOf(
            "add" to Value.FALSE, "rm" to Value.TRUE),
            docopt("Usage: prog (add|rm)",
                listOf("rm").toTypedArray()))
        assertEquals(
            mapOf("a" to Value.TRUE, "b" to Value.TRUE),
            docopt("Usage: prog a b",
                listOf("a", "b").toTypedArray()))
        assertRaises(DocoptExit::class.java) {
            docopt("Usage: prog a b", listOf("b", "a").toTypedArray())
        }
    }

    @Test
    fun testFormalUsage() {
        val doc = """
            |Usage: prog [-hv] ARG
            |       prog N M
            |
            |prog is a program.""".trimMargin()
        val usage = parseSection("usage:", doc).first()
        assertEquals("Usage: prog [-hv] ARG\n       prog N M", usage)
        assertEquals("( [-hv] ARG ) | ( N M )", formalUsage(usage))
    }

    @Test
    fun testParseArgv() {
        val o = listOf(
            Option("-h"),
            Option("-v", "--verbose"),
            Option("-f", "--file", 1))
        val ts = { s: String -> Tokens(s.split(" "), DocoptExit.FACTORY) }
        val parser = Parser("unit test")
        assertEquals(listOf<LeafPattern>(),
            parser.parseArgv(Tokens(listOf()), o).result)
        assertEquals(listOf(Option("-h", null, 0, Value.TRUE)),
            parser.parseArgv(ts("-h"), o).result)
        assertEquals(listOf(Option("-h", null, 0, Value.TRUE), Option("-v",
            "--verbose", 0, Value.TRUE)),
            parser.parseArgv(ts("-h --verbose"), o).result)
        assertEquals(listOf(Option("-h", null, 0, Value.TRUE), Option("-f",
            "--file", 1, Value.AString("f.txt"))),
            parser.parseArgv(ts("-h --file f.txt"), o).result)
        assertEquals(
            listOf(
                Option("-h", null, 0, Value.TRUE),
                Option("-f", "--file", 1, Value.AString("f.txt")),
                Argument(null, Value.AString("arg"))
            ), parser.parseArgv(ts("-h --file f.txt arg"), o).result)
        assertEquals(
            listOf(
                Option("-h", null, 0, Value.TRUE),
                Option("-f", "--file", 1, Value.AString("f.txt")),
                Argument(null, Value.AString("arg")),
                Argument(null, Value.AString("arg2"))
            ), parser.parseArgv(ts("-h --file f.txt arg arg2"), o).result)
        assertEquals(
            listOf(
                Option("-h", null, 0, Value.TRUE),
                Argument(null, Value.AString("arg")),
                Argument(null, Value.AString("--")),
                Argument(null, Value.AString("-v"))
            ), parser.parseArgv(ts("-h arg -- -v"), o).result)
        val usage = "usage: prog [-vv]"
        val options = parseDefaults(usage)
        val parser2 = Parser(usage)
        val parsedArgv = parser2.parseArgv(Tokens(listOf()), options, false)
        assertEquals(listOf<String>(), parsedArgv.result)
    }

    @Test
    fun testParsePattern() {
        val o = listOf(
            Option("-h"),
            Option("-v", "--verbose"),
            Option("-f", "--file", 1))
        val parser = Parser("UNIT TEST")
        assertEquals(Required(Optional(Option("-h"))),
            parser.parsePattern("[ -h ]", o))
        assertEquals(Required(Optional(Option("-h"))),
            parser.parsePattern("[ -h ]", options = o))
        assertEquals(Required(Optional(OneOrMore(Argument("ARG")))),
            parser.parsePattern("[ ARG ... ]", options = o))
        assertEquals(
            Required(Optional(Either(Option("-h"), Option("-v", "--verbose")))),
            parser.parsePattern("[ -h | -v ]", options = o))
        assertEquals(Required(Required(Either(Option("-h"),
            Required(Option("-v", "--verbose"),
                Optional(Option("-f", "--file", 1, null)))))),
            parser.parsePattern("( -h | -v [ --file <f> ] )", options = o))
        assertEquals(Required(Required(Either(Option("-h"),
            Required(Option("-v", "--verbose"),
                Optional(Option("-f", "--file", 1, null)),
                OneOrMore(Argument("N")))))),
            parser.parsePattern("(-h|-v[--file=<f>]N...)", options = o))
        assertEquals(Required(Required(Either(Required(Argument("N"), Optional(
            Either(Argument("M"),
                Required(Either(Argument("K"), Argument("L")))))),
            Required(Argument("O"), Argument("P"))))),
            parser.parsePattern("(N [M | (K | L)] | O P)", options = listOf()))
        assertEquals(Required(Optional(Option("-h")), Optional(Argument("N"))),
            parser.parsePattern("[ -h ] [N]", options = o))
        assertEquals(Required(Optional(OptionsShortcut())),
            parser.parsePattern("[options]", options = o))
        assertEquals(Required(Optional(OptionsShortcut()), Argument("A")),
            parser.parsePattern("[options] A", options = o))
        assertEquals(
            Required(Option("-v", "--verbose"), Optional(OptionsShortcut())),
            parser.parsePattern("-v [options]", options = o))
        assertEquals(Required(Argument("ADD")),
            parser.parsePattern("ADD", options = o))
        assertEquals(Required(Argument("<add>")),
            parser.parsePattern("<add>", options = o))
        assertEquals(Required(Command("add")),
            parser.parsePattern("add", options = o))
    }

    @Test
    fun parsePatternDoubleDash() {
        assertEquals(
            Required(
                Required(
                    Optional(
                        Option("-o", null, 0, Value.FALSE)),
                    Optional(Command("--", Value.FALSE)),
                    Argument("<arg>", null))),
            Parser("UNIT TEST").parsePattern("( [-o] [--] <arg> )", listOf())
        )
    }

    @Test
    fun testOptionMatch() {
        assertEquals(PatternMatchResult(true, listOf(),
            listOf(Option("-a", value = Value.TRUE))),
            Option("-a").match(
                listOf(Option("-a", value = Value.TRUE))))
        assertEquals(
            PatternMatchResult(false, listOf(Option("-x")), listOf()),
            Option("-a").match(listOf(Option("-x"))))
        assertEquals(
            PatternMatchResult(false, listOf(Argument("N")), listOf()),
            Option("-a").match(listOf(Argument("N"))))
        assertEquals(
            PatternMatchResult(true, listOf(Option("-x"), Argument("N")),
                listOf(Option("-a"))), Option("-a").match(
            listOf(Option("-x"), Option("-a"), Argument("N"))))
        assertEquals(
            PatternMatchResult(true, listOf(Option("-a")), listOf(Option("-a",
                value = Value.TRUE))),
            Option("-a").match(
                listOf(Option("-a", value = Value.TRUE), Option("-a"))))
    }

    @Test
    fun testArgumentMatch() {
        assertEquals(
            PatternMatchResult(true, listOf(),
                listOf(Argument("N", Value.AnInt(9)))),
            Argument("N").match(listOf(Argument(null, Value.AnInt(9)))))
        assertEquals(
            PatternMatchResult(false, listOf(Option("-x")), listOf()),
            Argument("N").match(listOf(Option("-x"))))
        assertEquals(
            PatternMatchResult(true, listOf(Option("-x"), Option("-a")),
                listOf(Argument("N",
                    Value.AnInt(5)))), Argument("N").match(
            listOf(Option("-x"), Option("-a"), Argument(null, Value.AnInt(5)))))
        assertEquals(
            PatternMatchResult(true, listOf(Argument(null, Value.AnInt(0))),
                listOf(Argument("N",
                    Value.AnInt(9)))),
            Argument("N").match(
                listOf(Argument(null, Value.AnInt(9)), Argument(null,
                    Value.AnInt(0)))))
    }

    @Test
    fun testCommandMatch() {
        assertEquals(
            PatternMatchResult(true, listOf(),
                listOf(Command("c", Value.TRUE))),
            Command("c").match(listOf(Argument(null, Value.AString("c")))))
        assertEquals(PatternMatchResult(false, listOf(Option("-x")), listOf()),
            Command("c").match(listOf(Option("-x"))))
        assertEquals(
            PatternMatchResult(true, listOf(Option("-x"), Option("-a")),
                listOf(Command("c", Value.TRUE))),
            Command("c").match(
                listOf(Option("-x"),
                    Option("-a"),
                    Argument(null, Value.AString("c")))))
        assertEquals(
            PatternMatchResult(true, listOf(),
                listOf(Command("rm", Value.TRUE))),
            Either(Command("add", Value.FALSE),
                Command("rm", Value.FALSE)).match(
                listOf(Argument(null, Value.AString("rm")))))
    }

    @Test
    fun testOptionalMatch() {
        assertEquals(PatternMatchResult(true, listOf(), listOf(Option("-a"))),
            Optional(Option("-a")).match(listOf(Option("-a"))))
        assertEquals(PatternMatchResult(true, listOf(), listOf()),
            Optional(Option("-a")).match(listOf()))
        assertEquals(PatternMatchResult(true, listOf(Option("-x")), listOf()),
            Optional(Option("-a")).match(listOf(Option("-x"))))
        assertEquals(PatternMatchResult(true, listOf(), listOf(Option("-a"))),
            Optional(Option("-a"), Option("-b")).match(listOf(Option("-a"))))
        assertEquals(PatternMatchResult(true, listOf(), listOf(Option("-b"))),
            Optional(Option("-a"), Option("-b")).match(listOf(Option("-b"))))
        assertEquals(PatternMatchResult(true, listOf(Option("-x")), listOf()),
            Optional(Option("-a"), Option("-b")).match(listOf(Option("-x"))))
        assertEquals(
            PatternMatchResult(true, listOf(), listOf(Argument("N", Value
                .AnInt(9)))),
            Optional(Argument("N")).match(listOf(
                Argument(null, Value.AnInt(9)))))
        assertEquals(PatternMatchResult(true, listOf(Option("-x")),
            listOf(Option("-a"), Option("-b"))),
            Optional(Option("-a"), Option("-b")).match(
                listOf(Option("-b"), Option("-x"), Option("-a"))))
    }

    @Test
    fun testRequiredMatch() {
        assertEquals(PatternMatchResult(true, listOf(), listOf(Option("-a"))),
            Required(Option("-a")).match(listOf(Option("-a"))))
        assertEquals(PatternMatchResult(false, listOf(), listOf()),
            Required(Option("-a")).match(listOf()))
        assertEquals(PatternMatchResult(false, listOf(Option("-x")), listOf()),
            Required(Option("-a")).match(listOf(Option("-x"))))
        assertEquals(PatternMatchResult(false, listOf(Option("-a")), listOf()),
            Required(Option("-a"), Option("-b")).match(listOf(Option("-a"))))
    }

    @Test
    fun testEitherMatch() {
        assertEquals(PatternMatchResult(true, listOf(), listOf(Option("-a"))),
            Either(Option("-a"), Option("-b")).match(listOf(Option("-a"))))
        assertEquals(PatternMatchResult(true, listOf(Option("-b")),
            listOf(Option("-a"))), Either(Option("-a"), Option("-b")).match(
            listOf(Option("-a"), Option("-b"))))
        assertEquals(PatternMatchResult(false, listOf(Option("-x")), listOf()),
            Either(Option("-a"), Option("-b")).match(listOf(Option("-x"))))
        assertEquals(PatternMatchResult(true, listOf(Option("-x")),
            listOf(Option("-b"))),
            Either(Option("-a"), Option("-b"), Option("-c")).match(
                listOf(Option("-x"), Option("-b"))))
        assertEquals(PatternMatchResult(true, listOf(),
            listOf(Argument("N", Value.AnInt(1)), Argument("M", Value.AnInt(2)
            ))),
            Either(Argument("M"), Required(Argument("N"), Argument("M"))).match(
                listOf(Argument(null, Value.AnInt(1)), Argument(null, Value
                    .AnInt(2)))))
    }

    @Test
    fun testOneOrMoreMatch() {
        assertEquals(
            PatternMatchResult(true, listOf(),
                listOf(Argument("N", Value.AnInt(9)))),
            OneOrMore(Argument("N")).match(
                listOf(Argument(null, Value.AnInt(9)))))
        assertEquals(PatternMatchResult(false, listOf(), listOf()),
            OneOrMore(Argument("N")).match(listOf()))
        assertEquals(PatternMatchResult(false, listOf(Option("-x")), listOf()),
            OneOrMore(Argument("N")).match(listOf(Option("-x"))))
        assertEquals(PatternMatchResult(true, listOf(),
            listOf(Argument("N", Value.AnInt(9)),
                Argument("N", Value.AnInt(8)))),
            OneOrMore(Argument("N")).match(
                listOf(Argument(null, Value.AnInt(9)),
                    Argument(null, Value.AnInt(8)))))
        assertEquals(PatternMatchResult(true, listOf(Option("-x")),
            listOf(Argument("N", Value.AnInt(9)),
                Argument("N", Value.AnInt(8)))),
            OneOrMore(Argument("N")).match(
                listOf(Argument(null, Value.AnInt(9)), Option("-x"),
                    Argument(null, Value.AnInt(8)))))
        assertEquals(
            PatternMatchResult(true, listOf(Argument(null, Value.AnInt(8))),
                listOf(Option("-a"), Option("-a"))),
            OneOrMore(Option("-a")).match(
                listOf(Option("-a"), Argument(null, Value.AnInt(8)),
                    Option("-a"))))
        assertEquals(
            PatternMatchResult(false,
                listOf(Argument(null, Value.AnInt(8)), Option("-x")),
                listOf()),
            OneOrMore(Option("-a")).match(
                listOf(Argument(null, Value.AnInt(8)), Option("-x"))))
        assertEquals(PatternMatchResult(true, listOf(Option("-x")),
            listOf(Option("-a"), Argument("N", Value.AnInt(1)), Option("-a"),
                Argument("N", Value.AnInt(2)))),
            OneOrMore(Required(Option("-a"), Argument("N"))).match(
                listOf(Option("-a"), Argument(null, Value.AnInt(1)),
                    Option("-x"),
                    Option("-a"), Argument(null, Value.AnInt(2)))))
        assertEquals(
            PatternMatchResult(true, listOf(),
                listOf(Argument("N", Value.AnInt(9)))),
            OneOrMore(Optional(Argument("N"))).match(
                listOf(Argument(null, Value.AnInt(9)))))
    }

    @Test
    fun testListArgumentMatch() {
        assertEquals(PatternMatchResult(true, listOf(),
            listOf(Argument("N", Value.AList(listOf("1", "2"))))),
            Required(Argument("N"), Argument("N")).fix()
                .match(listOf(Argument(null, Value.AString("1")),
                    Argument(null, Value.AString("2")))))
        assertEquals(PatternMatchResult(true, listOf(),
            listOf(Argument("N", Value.AList(listOf("1", "2", "3"))))),
            OneOrMore(Argument("N")).fix().match(
                listOf(Argument(null, Value.AString("1")),
                    Argument(null, Value.AString("2")),
                    Argument(null, Value.AString("3")))))
        assertEquals(PatternMatchResult(true, listOf(),
            listOf(Argument("N", Value.AList(listOf("1", "2", "3"))))),
            Required(Argument("N"), OneOrMore(Argument("N"))).fix().match(
                listOf(Argument(null, Value.AString("1")),
                    Argument(null, Value.AString("2")),
                    Argument(null, Value.AString("3")))))
        assertEquals(PatternMatchResult(true, listOf(),
            listOf(Argument("N", Value.AList(listOf("1", "2"))))),
            Required(Argument("N"), Required(Argument("N"))).fix().match(
                listOf(Argument(null, Value.AString("1")),
                    Argument(null, Value.AString("2")))))
    }

    @Test
    fun testBasicPatternMatching() {
        // ( -a N [ -x Z ] )
        val pattern = Required(Option("-a"),
            Argument("N"),
            Optional(Option("-x"), Argument("Z")))
        // -a N
        assertEquals(
            PatternMatchResult(true, listOf(),
                listOf(Option("-a"), Argument("N", Value.AnInt(9)))),
            pattern.match(listOf(Option("-a"), Argument(null, Value.AnInt(9)))))
        // -a -x N Z
        assertEquals(
            PatternMatchResult(true, listOf(),
                listOf(Option("-a"), Argument("N", Value.AnInt(9)),
                    Option("-x"), Argument("Z", Value.AnInt(5)))),
            pattern.match(listOf(Option("-a"), Option("-x"),
                Argument(null, Value.AnInt(9)),
                Argument(null, Value.AnInt(5)))))
        // -x N Z  // BZZ!
        assertEquals(
            PatternMatchResult(false,
                listOf(Option("-x"), Argument(null, Value.AnInt(9)),
                    Argument(null, Value.AnInt(5))), listOf()),
            pattern.match(listOf(Option("-x"), Argument(null, Value.AnInt(9)),
                Argument(null, Value.AnInt(5)))))
    }

    @Test
    fun testPatternEither() {
        assertEquals(Either(Required(Option("-a"))),
            Pattern.transform(Option("-a")))
        assertEquals(Either(Required(Argument("A"))),
            Pattern.transform(Argument("A")))
        assertEquals(Either(
            Required(Option("-a"), Option("-c")),
            Required(Option("-b"), Option("-c"))),
            Pattern.transform(Required(Either(
                Option("-a"), Option("-b")),
                Option("-c"))))
        assertEquals(Either(Required(Option("-b"), Option("-a")),
            Required(Option("-c"), Option("-a"))),
            Pattern.transform(Optional(Option("-a"), Either(Option("-b"), Option
            ("-c")))))
        assertEquals(Either(Required(Option("-x")), Required(Option("-y")),
            Required(Option("-z"))),
            Pattern.transform(
                Either(Option("-x"), Either(Option("-y"), Option("-z")
                ))))
        assertEquals(Either(
            Required(Argument("N"), Argument("M"), Argument("N"),
                Argument("M"))),
            Pattern.transform(OneOrMore(Argument("N"), Argument("M"))))
    }

    @Test
    fun testPatternFixRepeatingArguments() {
        assertEquals(Option("-a"), Option("-a").fixRepeatingArguments())
        assertEquals(Argument("N", null),
            Argument("N", null).fixRepeatingArguments())
        assertEquals(Required(Argument("N", Value.AList(listOf())),
            Argument("N", Value.AList(listOf()))),
            Required(Argument("N"), Argument("N")).fixRepeatingArguments())
        assertEquals(
            Either(Argument("N", Value.AList(listOf())),
                OneOrMore(Argument("N", Value.AList(listOf())))),
            Either(Argument("N"), OneOrMore(Argument("N"))).fix())
    }

    @Test
    fun testSet() {
        assertEquals(Argument("N"), Argument("N"))
        assertEquals(setOf(Argument("N")),
            setOf(Argument("N"), Argument("N")))
    }

    @Test
    fun testPatternFixIdentities1() {
        var pattern: Pattern = Required(Argument("N"), Argument("N"))
        assertEquals(pattern.children()!![1], pattern.children()!![0])
        assertNotSame(pattern.children()!![1], pattern.children()!![0])
        pattern = pattern.fixIdentities()
        Assert.assertSame(pattern.children()!![0], pattern.children()!![1])
    }

    @Test
    fun testPatternFixIdentities2() {
        var pattern: Pattern = Required(
            Optional(Argument("X"), Argument("N")),
            Argument("N"))
        assertEquals(pattern.children()!![1],
            pattern.children()!![0].children()!![1])
        assertNotSame(pattern.children()!![1],
            pattern.children()!![0].children()!![1])
        pattern = pattern.fixIdentities()
        assertSame(pattern.children()!![1],
            pattern.children()!![0].children()!![1])
    }

    @Test
    fun testLongOptionsErrorHandling() {
//        with raises(DocoptLanguageError):
//            docopt("Usage: prog --non-existent", "--non-existent")
//        with raises(DocoptLanguageError):
//            docopt("Usage: prog --non-existent")
        assertRaises(DocoptExit::class.java) {
            docopt("Usage: prog", arrayOf("--non-existent"))
        }

        assertRaises(DocoptExit::class.java) {
            docopt("Usage: prog [--version --verbose]\n",
                arrayOf("Options: --version\n --verbose", "--ver"))
        }

        assertRaises(DocoptLanguageError::class.java) {
            docopt("Usage: prog --long\nOptions: --long ARG")
        }

        assertRaises(DocoptExit::class.java) {
            docopt("Usage: prog --long ARG\nOptions: --long ARG",
                arrayOf("--long"))
        }

        assertRaises(DocoptLanguageError::class.java) {
            docopt("Usage: prog --long=ARG\nOptions: --long")
        }

        assertRaises(DocoptExit::class.java) {
            docopt("Usage: prog --long\nOptions: --long", arrayOf("--long=ARG"))
        }
    }

    @Test
    fun testShortOptionsErrorHandling() {
        assertRaises(DocoptLanguageError::class.java) {
            docopt("Usage: prog -x\nOptions: -x  this\n -x  that")
        }
//        with raises (DocoptLanguageError):
//           docopt("Usage: prog -x")
        assertRaises(DocoptExit::class.java) {
            docopt("Usage: prog", arrayOf("-x"))
        }
        assertRaises(DocoptLanguageError::class.java) {
            docopt("Usage: prog -o\nOptions: -o ARG")
        }
        assertRaises(DocoptExit::class.java) {
            docopt("Usage: prog -o ARG\nOptions: -o ARG", arrayOf("-o"))
        }
    }

    @Test
    fun testMatchingParen() {
        assertRaises(DocoptLanguageError::class.java) {
            docopt("Usage: prog [a [b]")
        }
        assertRaises(DocoptLanguageError::class.java) {
            docopt("Usage: prog [a [b] ] c )")
        }
    }

    @Test
    fun testAllowDoubleDash() {
        assertEquals(
            mapOf("-o" to Value.FALSE,
                "<arg>" to Value.AString("-o"),
                "--" to Value.TRUE),
            docopt("usage: prog [-o] [--] <arg>\nkptions: -o",
                arrayOf("--", "-o")))
        assertEquals(
            mapOf("-o" to Value.TRUE,
                "<arg>" to Value.AString("1"),
                "--" to Value.FALSE),
            docopt("usage: prog [-o] [--] <arg>\nkptions: -o",
                arrayOf("-o", "1")))
        assertRaises(DocoptExit::class.java) {
            docopt("usage: prog [-o] <arg>\noptions:-o", arrayOf("--", "-o"))
        }
    }

    @Test
    fun testDocopt() {
        with("""Usage: prog [-v] A

                     Options: -v  Be verbose.""") {
            assertEquals(
                mapOf("-v" to Value.FALSE, "A" to Value.AString("arg")),
                docopt(this, arrayOf("arg"))
            )
            assertEquals(
                mapOf("-v" to Value.TRUE, "A" to Value.AString("arg")),
                docopt(this, arrayOf("-v", "arg"))
            )
        }

        with("""Usage: prog [-vqr] [FILE]
                         prog INPUT OUTPUT
                         prog --help

                         Options:
                         -v  print status messages
                         -q  report only file names
                         -r  show all occurrences of the same error
                         --help
                         """) {
            val a = docopt(this, arrayOf("-v", "file.py"))
            assertEquals(
                mapOf("-v" to Value.TRUE, "-q" to Value.FALSE, "-r" to Value
                    .FALSE, "--help" to Value.FALSE, "FILE" to Value.AString
                ("file.py"), "INPUT" to null, "OUTPUT" to null),
                a
            )
        }
    }

    @Test
    fun testLanguageErrors() {
        assertRaises(DocoptLanguageError::class.java) {
            docopt("no usage with colon here")
        }
        assertRaises(DocoptLanguageError::class.java) {
            docopt("usage: here \n\n and again usage: here")
        }
    }

    @Test
    fun testIssue40() {
        assertRaises(DocoptExit::class.java) {
            docopt("usage: prog --help-commands | --help", "--help")
        }

        Assert.assertEquals(
            mapOf("--aabb" to Value.FALSE, "--aa" to Value.TRUE),
            docopt("usage: prog --aabb | --aa", "--aa"))
    }

    /**
     * Java/Kotlin does not have the same "byte string" vs "unicode string"
     * concept as python, so this test is left commented out.
     */
    @Test
    fun testIssue34UnicodeStrings() {
//    try:
//    assert docopt(eval("u"usage: prog [-o <a>]""), "") ==  { "-o": false, "<a>": null }
//    except SyntaxError:
//    pass  # Python 3
    }

    @Test
    fun testCountMultipleFlags() {
        Assert.assertEquals(mapOf("-v" to Value.TRUE),
            docopt("usage: prog [-v]", "-v"))
        Assert.assertEquals(mapOf("-v" to Value.AnInt(0)),
            docopt("usage: prog [-vv]", arrayOf()))
        Assert.assertEquals(mapOf("-v" to Value.AnInt(1)),
            docopt("usage: prog [-vv]", "-v"))
        Assert.assertEquals(mapOf("-v" to Value.AnInt(2)),
            docopt("usage: prog [-vv]", "-vv"))
        assertRaises(DocoptExit::class.java) {
            docopt("usage: prog [-vv]", "-vvv")
        }
        Assert.assertEquals(mapOf("-v" to Value.AnInt(3)),
            docopt("usage: prog [-v | -vv | -vvv]", "-vvv"))
        Assert.assertEquals(mapOf("-v" to Value.AnInt(6)),
            docopt("usage: prog -v...", "-vvvvvv"))
        Assert.assertEquals(mapOf("--ver" to Value.AnInt(2)),
            docopt("usage: prog [--ver --ver]", arrayOf("--ver", "--ver")))
    }

    @Test
    fun testAnyOptionsParameter() {
        assertRaises(DocoptExit::class.java) {
            docopt("usage: prog [options]",
                arrayOf("-foo", "--bar", "--spam=eggs"))
        }
        assertRaises(DocoptExit::class.java) {
            docopt("usage: prog [options]",
                arrayOf("--foo", "--bar", "--bar"))
        }
        assertRaises(DocoptExit::class.java) {
            docopt("usage: prog [options]",
                arrayOf("--bar", "--bar", "--bar", "-ffff"))
        }
        assertRaises(DocoptExit::class.java) {
            docopt("usage: prog [options]", arrayOf("--long=arg",
                "--long=another"))
        }
    }

    @Test
    fun testDefaultValueForPositionalArguments() {
        var doc = """Usage: prog [--data=<data>...]

                     Options:
                         -d --data=<arg>    Input data [default: x]
                  """
        var a = docopt(doc, arrayOf())
        Assert.assertEquals(mapOf("--data" to Value.AList(listOf("x"))), a)
        doc = """Usage: prog [--data=<data>...]

                 Options:
                     -d --data=<arg>    Input data [default: x y]
              """
        a = docopt(doc, arrayOf())
        Assert.assertEquals(mapOf("--data" to Value.AList(listOf("x", "y"))), a)
        doc = """Usage: prog [--data=<data>...]

                 Options:
                     -d --data=<arg>    Input data [default: x y]
              """
        a = docopt(doc, "--data=this")
        Assert.assertEquals(mapOf("--data" to Value.AList(listOf("this"))), a)
    }

    @Test
    fun parseDefaults() {
        assertEquals(
            listOf(Option(null, "--long", 1, null)),
            parseDefaults("Usage: prog --long\nOptions: --long ARG")
        )
        val doc = "usage: prog [-ab] [options]\noptions: -x\n -y"
        assertEquals(
            listOf(Option("-x", null, 0), Option("-y", null, 0)),
            parseDefaults(doc)
        )
    }

    @Test
    fun testIssue59() {
        Assert.assertEquals(
            mapOf("--long" to Value.AString("")),
            docopt("usage: prog --long=<a>", "--long="))
        Assert.assertEquals(
            mapOf("-l" to Value.AString("")),
            docopt("usage: prog -l <a>\noptions: -l <a>", arrayOf("-l", "")))
    }

    @Test
    fun testOptionsFirst() {
        Assert.assertEquals(
            mapOf("--opt" to Value.TRUE,
                "<args>" to Value.AList(listOf("this", "that"))),
            docopt("usage: prog [--opt] [<args>...]",
                arrayOf("--opt", "this", "that")))
        Assert.assertEquals(
            mapOf("--opt" to Value.TRUE,
                "<args>" to Value.AList(listOf("this", "that"))),
            docopt("usage: prog [--opt] [<args>...]",
                arrayOf("this", "that", "--opt")))
        Assert.assertEquals(
            mapOf("--opt" to Value.FALSE,
                "<args>" to Value.AList(listOf("this", "that", "--opt"))),
            docopt("usage: prog [--opt] [<args>...]",
                arrayOf("this", "that", "--opt"),
                optionsFirst = true))
    }

    @Test
    fun testIssue68OptionsShortcutDoesNotIncludeOptionsInUsagePattern() {
        val args = docopt("usage: prog [-ab] [options]\n" +
            "options: -x\n -y", "-ax")
        // Need to use `is` (not `==`) since we want to make sure
        // that they are not 1/0, but strictly true/false:
        Assert.assertEquals(Value.TRUE, args["-a"])
        Assert.assertEquals(Value.FALSE, args["-b"])
        Assert.assertEquals(Value.TRUE, args["-x"])
        Assert.assertEquals(Value.FALSE, args["-y"])
    }

    @Test
    fun testIssue65EvaluateArgvWhenCalledNotWhenImported() {
        Assert.assertEquals(mapOf("-a" to Value.TRUE, "-b" to Value.FALSE),
            docopt("usage: prog [-ab]", arrayOf("-a")))
        Assert.assertEquals(mapOf("-a" to Value.FALSE, "-b" to Value.TRUE),
            docopt("usage: prog [-ab]", arrayOf("-b")))
    }

    @Test
    fun testIssue71DoubleDashIsNotA_validOptionArgument() {
        assertRaises(DocoptExit::class.java) {
            docopt("usage: prog [--log=LEVEL] [--] <args>...", "--log -- 1 2")
        }
        assertRaises(DocoptExit::class.java) {
            docopt(
                """usage: prog [-l LEVEL] [--] <args>...  options: -l LEVEL""",
                "-l -- 1 2")
        }
    }

    @Test
    fun testParseSection() {
        val usage = """usage: this
        |
        |usage:hai
        |usage: this that
        |
        |usage: foo
        |       bar
        |
        |PROGRAM USAGE:
        | foo
        | bar
        |usage:
        |${"\t"}too
        |${"\t"}tar
        |Usage: eggs spam
        |BAZZ
        |usage: pit stop""".trimMargin()

        assertEquals(
            listOf<String>(),
            parseSection(USAGE_SECTION, "foo bar fizz buzz").toList())
        assertEquals(
            listOf("usage: prog"),
            parseSection(USAGE_SECTION, "usage: prog").toList())
        assertEquals(
            listOf("usage: -x\n -y"),
            parseSection(USAGE_SECTION, "usage: -x\n -y").toList())
        assertEquals(
            listOf(
                "usage: this",
                "usage:hai",
                "usage: this that",
                "usage: foo\n       bar",
                "PROGRAM USAGE:\n foo\n bar",
                "usage:\n\ttoo\n\ttar",
                "Usage: eggs spam",
                "usage: pit stop"
            ), parseSection(USAGE_SECTION, usage).toList())
    }

    @Test
    fun testIssue126DefaultsNotParsedCorrectlyWhenTabs() {
        val section = "Options:\n\t--foo=<arg>  [default: bar]"
        assertArrayEquals(arrayOf(
            Option(null, "--foo", 1, Value.AString("bar"))),
            parseDefaults(section).toList().toTypedArray())
    }
}
