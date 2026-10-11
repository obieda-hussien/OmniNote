package com.omninote

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.omninote.ui.screens.copyUriToInternalStorage
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AttachmentImportTest {
    @Test fun internalImportsAreCompleteAndHaveDistinctNames() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File(context.cacheDir, "image (one) [2].jpg").apply { writeBytes(ByteArray(150000) { (it % 255).toByte() }) }
        val a = File(copyUriToInternalStorage(context, Uri.fromFile(source), source.name).path!!)
        val b = File(copyUriToInternalStorage(context, Uri.fromFile(source), source.name).path!!)
        try {
            assertNotEquals(a.path, b.path)
            assertArrayEquals(source.readBytes(), a.readBytes())
            assertArrayEquals(source.readBytes(), b.readBytes())
            assertEquals(context.filesDir.canonicalPath, a.parentFile!!.canonicalPath)
        } finally { a.delete(); b.delete(); source.delete() }
    }
    @Test fun inaccessibleImportsFailWithoutReturningBrokenUrisOrLeavingPartialFiles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val before = context.filesDir.listFiles().orEmpty().map { it.name }.toSet()
        val missing = File(context.cacheDir, "missing-image.jpg").apply { delete() }
        assertTrue(runCatching { copyUriToInternalStorage(context, Uri.fromFile(missing), "missing-image.jpg") }.isFailure)
        assertEquals(before, context.filesDir.listFiles().orEmpty().map { it.name }.toSet())
    }
}
