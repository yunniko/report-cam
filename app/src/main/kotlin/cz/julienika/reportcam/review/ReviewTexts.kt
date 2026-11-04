package cz.julienika.reportcam.review

import android.text.InputType
import android.text.style.SuggestionSpan
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.isVisible
import cz.julienika.reportcam.databinding.ActivityCameraBinding

/**
 * The review's two text rows (Owner, G-004): the Note, printed on the stamp, and the Message, sent with the
 * photo. A row switched off shows only its name and switch. A note edit counts once it is finished (keyboard
 * Done, leaving the field, the switch); then [onNoteChanged] asks for the photo to be drawn again.
 * In [compact] mode a field reads as plain text until tapped, and goes back to plain text when the edit ends.
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

    private val fields = listOf(ui.noteText, ui.messageText)

    /** The field's own look (its underline), put back while it is edited in compact mode. */
    private val underlines = fields.associateWith { it.background }

    /** The Settings "Compact text fields" (Owner, G-004): fields read as plain text until tapped. */
    var compact = false
        set(on) {
            field = on
            fields.forEach { if (on && !it.hasFocus()) plain(it) else editable(it) }
        }

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
        ui.noteText.setOnFocusChangeListener { _, focused -> if (!focused) letGo(ui.noteText) }
        ui.messageText.setOnFocusChangeListener { _, focused -> if (!focused) letGo(ui.messageText) }
        fields.forEach { f -> f.setOnClickListener { if (compact && !f.hasFocus()) startEditing(f) } }
        ui.noteSwitch.setOnCheckedChangeListener { _, on ->
            ui.noteText.isVisible = on
            commit()
        }
        ui.messageSwitch.setOnCheckedChangeListener { _, on -> ui.messageText.isVisible = on }
        // Somewhere for the focus to go when a field lets go of it, so it does not jump to the other field;
        // clickable, so a tap beside the fields ends an edit too.
        ui.reviewControls.isFocusableInTouchMode = true
        ui.reviewControls.isClickable = true
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
        if (fields.any { it.hasFocus() }) ui.reviewControls.requestFocus() // letGo closes the keyboard
        commit()
    }

    private var keyboardWasShown = false

    /** Closing the keyboard (Back, its own button) ends an edit in compact mode: the field reads as text again. */
    fun keyboardShown(shown: Boolean) {
        if (keyboardWasShown && !shown && compact) finishEditing()
        keyboardWasShown = shown
    }

    /** A field lost the focus: back to plain text in compact mode, the keyboard closed unless the other has it. */
    private fun letGo(f: EditText) {
        if (compact) plain(f)
        if (fields.none { it.hasFocus() }) keyboard()?.hideSoftInputFromWindow(ui.root.windowToken, 0)
        if (f === ui.noteText) commit()
    }

    private fun startEditing(f: EditText) {
        editable(f)
        f.requestFocus()
        f.setSelection(f.length())
        keyboard()?.showSoftInput(f, InputMethodManager.SHOW_IMPLICIT)
    }

    /** Plain text: no underline, cursor or spelling marks, and a tap (not the focus) starts an edit. The padding stays. */
    private fun plain(f: EditText) {
        f.isFocusable = false
        f.isCursorVisible = false
        f.background = null
        // The spell checker marks only fields with suggestions, and its marks are noise in plain text.
        f.setRawInputType(f.inputType or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        f.text.getSpans(0, f.length(), SuggestionSpan::class.java).forEach { f.text.removeSpan(it) }
    }

    private fun editable(f: EditText) {
        f.isFocusable = true
        f.isFocusableInTouchMode = true
        f.isCursorVisible = true
        f.background = underlines[f]
        f.setRawInputType(f.inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS.inv())
    }

    private fun keyboard() = ui.root.context.getSystemService(InputMethodManager::class.java)

    private fun commit() {
        if (filling || (note == committedNote && noteOn == committedNoteOn)) return
        committedNote = note
        committedNoteOn = noteOn
        onNoteChanged()
    }
}
