package org.winlogon.echolite

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringExtensionSpec {

    @Test
    fun emptyStringRemainsEmpty() {
        assertEquals("", "".getDiscordCompatible())
    }

    @Test
    fun stringWithoutAnyUnderscoresRemainsUnchanged() {
        val original = "HelloWorld123"
        assertEquals(original, original.getDiscordCompatible())
    }

    @Test
    fun singleUnderscoreAtTheVeryBeginningIsEscaped() {
        val original = "_hello"
        // the underscore at index 0 is not preceded by '_' and not followed by '_', so it becomes "\_"
        assertEquals("\\_hello", original.getDiscordCompatible())
    }

    @Test
    fun singleUnderscoreAtTheVeryEndIsEscaped() {
        val original = "hello_"
        // the underscore at the end is not preceded by '_' and not followed by '_', so it becomes "\_"
        assertEquals("hello\\_", original.getDiscordCompatible())
    }

    @Test
    fun singleUnderscoreInTheMiddleIsEscaped() {
        val original = "hello_world"
        // the '_' between "hello" and "world" is not adjacent to any other '_', so it becomes "\_"
        assertEquals("hello\\_world", original.getDiscordCompatible())
    }

    @Test
    fun doubleUnderscoreRemainsUnchanged() {
        val original = "hello__world"
        // neither of those two underscores match '(?<!_)_(?!_)', because each one is either preceded or followed by '_'
        assertEquals(original, original.getDiscordCompatible())
    }

    @Test
    fun multipleSingleUnderscoresGetEscapedIndividually() {
        val original = "a_b_c"
        // both underscores are standalone, so each should become "\_"
        assertEquals("a\\_b\\_c", original.getDiscordCompatible())
    }

    @Test
    fun tripleUnderscoreRemainsUnchanged() {
        val original = "hello___world"
        // any character in that "___" fails either (?<!_) or (?!_), so none get replaced
        assertEquals(original, original.getDiscordCompatible())
    }

    @Test
    fun underscoreRightNextToALetterAtBothEndsBothGetEscaped() {
        val original = "_a_"
        // index 0: not preceded by '_' and next char is 'a' => escape it
        // index 2: preceded by 'a' and no following char => escape it
        assertEquals("\\_a\\_", original.getDiscordCompatible())
    }

    @Test
    fun mixedPatternALeavesTheFirstTwoTogetherButEscapesTheFinalOne() {
        val original = "__a_"
        // positions:
        //   index 0: '_' but next char is '_' ⇒ (?!_) fails ⇒ not replaced
        //   index 1: '_' but previous char is '_' ⇒ (?<!_) fails ⇒ not replaced
        //   index 3: '_' is standalone ⇒ replaced
        val expected = "__a\\_"
        assertEquals(expected, original.getDiscordCompatible())
    }
}
