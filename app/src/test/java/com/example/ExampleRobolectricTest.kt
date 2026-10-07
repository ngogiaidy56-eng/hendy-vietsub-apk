package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ExportFormat
import com.example.data.ScriptCadenceMode
import com.example.data.SubtitleParserExporter
import com.example.data.SubtitleProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Hendy Vietsub", appName)
    }

    @Test
    fun `parse and export SRT and ASS karaoke subtitles`() {
        val sampleSrt = """
            1
            00:00:00,000 --> 00:00:02,500
            Stop scrolling right now!
            ¡Deja de deslizar ahora!

            2
            00:00:02,600 --> 00:00:05,200
            Word by word karaoke pop
        """.trimIndent()

        val parsed = SubtitleParserExporter.parseSrtOrVtt(sampleSrt, projectId = 1L)
        assertEquals(2, parsed.size)
        assertEquals("Stop scrolling right now!", parsed[0].text)
        assertEquals("¡Deja de deslizar ahora!", parsed[0].secondaryText)
        assertEquals(4, parsed[0].parsedWords().size)

        val project = SubtitleProjectEntity(id = 1L, title = "Test Cut", isUppercase = true)
        val assOutput = SubtitleParserExporter.exportSubtitles(project, parsed, ExportFormat.ASS)
        assertTrue(assOutput.contains("[V4+ Styles]"))
        assertTrue(assOutput.contains("{\\kf"))

        val autoSynced = SubtitleParserExporter.autoSyncScript(
            rawScript = "First punchy hook! Second viral line with 100% retention.",
            projectId = 1L,
            cadence = ScriptCadenceMode.VIRAL_FAST
        )
        assertTrue(autoSynced.size >= 2)
    }
}
