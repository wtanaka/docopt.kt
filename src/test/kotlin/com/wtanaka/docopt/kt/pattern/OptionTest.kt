package com.wtanaka.docopt.kt.pattern

import com.wtanaka.docopt.kt.pattern.leaf.Option
import org.junit.Assert.assertEquals
import org.junit.Test

class OptionTest {
    @Test
    fun testOption() {
        assertEquals(Option("-h",
            null), Option.parse("-h"))
        assertEquals(Option(null,
            "--help"), Option.parse("--help"))
        assertEquals(Option("-h",
            "--help"), Option.parse("-h --help"))
        assertEquals(Option("-h",
            "--help"), Option.parse("-h, --help"))
        assertEquals(Option("-h", null,
            1), Option.parse("-h TOPIC"))
        assertEquals(Option(null, "--help",
            1), Option.parse("--help TOPIC"))
        assertEquals(
            Option("-h", "--help", 1),
            Option.parse("-h TOPIC --help TOPIC"))
        assertEquals(
            Option("-h", "--help", 1),
            Option.parse("-h TOPIC, --help TOPIC"))
        assertEquals(
            Option("-h", "--help", 1),
            Option.parse("-h TOPIC, --help=TOPIC"))
        assertEquals(Option("-h",
            null), Option.parse("-h  Description..."))
        assertEquals(Option("-h", "--help"),
            Option.parse("-h --help  Description..."))
        assertEquals(Option("-h", null, 1),
            Option.parse("-h TOPIC  Description..."))
        assertEquals(Option("-h",
            null), Option.parse("    -h"))
        assertEquals(
            Option("-h", null, 1, Value.AString("2")),
            Option.parse("-h TOPIC  Descripton... [default: 2]"))
        assertEquals(
            Option("-h", null, 1, Value.AString("topic-1")),
            Option.parse("-h TOPIC  Descripton... [default: topic-1]"))
        assertEquals(
            Option(null, "--help", 1,
                Value.AString("3.14")),
            Option.parse("--help=TOPIC  ... [default: 3.14]"))
        assertEquals(
            Option("-h", "--help", 1, Value.AString("./")),
            Option.parse("-h, --help=DIR  ... [default: ./]"))
        assertEquals(
            Option("-h", null, 1, Value.AString("2")),
            Option.parse("-h TOPIC  Descripton... [dEfAuLt: 2]"))
    }

    @Test
    fun testOptionName() {
        assertEquals("-h", Option("-h",
            null).name)
        assertEquals("--help", Option("-h",
            "--help").name)
        assertEquals("--help", Option(null,
            "--help").name)
    }
}
