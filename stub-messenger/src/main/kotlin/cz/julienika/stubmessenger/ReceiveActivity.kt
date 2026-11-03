package cz.julienika.stubmessenger

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.TextView

/** Shows and logs what a share delivered: the caption text and how many photo bytes could be read. */
class ReceiveActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)

        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<Uri>(android.content.Intent.EXTRA_STREAM)
        val bytes = uri?.let { u -> runCatching { contentResolver.openInputStream(u)?.use { it.readBytes().size } }.getOrNull() }
        val line = "received package=$packageName text=${text?.replace("\n", "\n")} bytes=$bytes"
        Log.i("StubMessenger", line)
        setContentView(TextView(this).apply { this.text = line })
    }
}
