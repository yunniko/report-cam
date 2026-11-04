package cz.julienika.reportcam.review

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import cz.julienika.reportcam.databinding.ActivityCameraBinding

/**
 * The review's two text rows (Owner, G-004): the Note, printed on the stamp, and the Message, sent with the
 * photo. A row switched off shows only its name and switch. A note edit counts once it is finished (keyboard
 * Done, leaving the field, the switch); then [onNoteChanged] asks for the photo to be drawn again.
 */
class ReviewTexts(
    private val ui: ActivityCameraBinding,
    private val onNoteChanged: () -> Unit,
) {
    /** The note as last finished: what the photo should show. Typing in progress is not in it. */
    private var committedNote = ""
    private var committedNoteOn = true

    /** Set while [show] fills the rows, so filling them is not taken for an edit. */
    private var filling = false

    /** The note the photo should carry: null when it is off or blank. */
    val stampedNote: String? get() = if (committedNoteOn) StampFacts.stampedNote(committedNote) else null

    val note: String get() = ui.noteText.text.toString()
    val noteOn: Boolean get() = ui.noteSwitch.isChecked
    val message: String get() = ui.messageText.text.toString()
    val messageOn: Boolean get() = ui.messageSwitch.isChecked

    /** The text that goes with the photo: null when the Message row is off or empty. */
    val messageToSend: String? get() = if (messageOn) message.trim().ifEmpty { null } else null

    init {
        // Shown on several lines, but the keyboard's action key is Done: it finishes the edit (no line breaks).
        ui.noteText.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        ui.noteText.setOnEditorActionListener { _, action, _ ->
            if (action != EditorInfo.IME_ACTION_DONE) return@setOnEditorActionListener false
            finishEditing()
            true
        }
        ui.noteText.setOnFocusChangeListener { _, focused -> if (!focused) commit() }
        ui.noteSwitch.setOnCheckedChangeListener { _, on ->
            ui.noteText.isVisible = on
            commit()
        }
        ui.messageSwitch.setOnCheckedChangeListener { _, on -> ui.messageText.isVisible = on }
        // Somewhere for the focus to go when a field lets go of it, so it does not jump to the other field.
        ui.reviewControls.isFocusableInTouchMode = true
    }

    /** Fills both rows for a photo coming into review: a new one, or one restored after a restart. */
    fun show(
        note: String,
        noteOn: Boolean,
        message: String,
        messageOn: Boolean,
    ) {
        filling = true
        ui.noteText.setText(note)
        ui.noteSwitch.isChecked = noteOn
        ui.noteText.isVisible = noteOn
        ui.messageText.setText(message)
        ui.messageSwitch.isChecked = messageOn
        ui.messageText.isVisible = messageOn
        committedNote = note
        committedNoteOn = noteOn
        filling = false
    }

    /**
     * The message follows the note (Owner, G-004): while it still reads [oldAuto], the stamp text it was
     * filled with, it becomes [newAuto]. One the user edited by hand is left alone.
     */
    fun follow(
        oldAuto: String,
        newAuto: String,
    ) {
        if (message == oldAuto && oldAuto != newAuto) ui.messageText.setText(newAuto)
    }

    /** Ends typing in either field: the keyboard closes and a note edit counts. */
    fun finishEditing() {
        if (ui.noteText.hasFocus() || ui.messageText.hasFocus()) {
            ui.reviewControls.requestFocus()
            val imm = ui.root.context.getSystemService(InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(ui.root.windowToken, 0)
        }
        commit()
    }

    private fun commit() {
        if (filling || (note == committedNote && noteOn == committedNoteOn)) return
        committedNote = note
        committedNoteOn = noteOn
        onNoteChanged()
    }
}
