package com.example.kaizenkanban.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {

    @Test
    fun dictationBecomesTitle() {
        val cmd = VoiceCommandParser.parse("купить молоко")
        assertTrue(cmd is VoiceCommand.AddTask)
        assertEquals("купить молоко", cmd!!.title)
    }

    @Test
    fun stripsOptionalAddPrefix() {
        val cmd = VoiceCommandParser.parse("добавь задачу купить молоко")
        assertTrue(cmd is VoiceCommand.AddTask)
        assertEquals("купить молоко", cmd!!.title)
    }

    @Test
    fun parsesEnglishWithWakeWord() {
        val cmd = VoiceCommandParser.parse("Ok Kairos, write report")
        assertTrue(cmd is VoiceCommand.AddTask)
        assertEquals("write report", cmd!!.title)
    }

    @Test
    fun stripsEnglishAddTaskPrefix() {
        val cmd = VoiceCommandParser.parse("add task write report")
        assertTrue(cmd is VoiceCommand.AddTask)
        assertEquals("write report", cmd!!.title)
    }

    @Test
    fun rejectsBlank() {
        assertNull(VoiceCommandParser.parse("   "))
    }
}
