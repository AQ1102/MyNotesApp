package com.example.notes

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: NotesAdapter
    private val notes = mutableListOf<Note>()

    companion object {
        private const val PREFS_NAME = "notes_prefs"
        private const val KEY_NOTES = "notes_json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        rv = findViewById(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = NotesAdapter(notes,
            onClick = { note -> showEditDialog(note) },
            onLongClick = { note -> confirmDelete(note) }
        )
        rv.adapter = adapter

        loadNotes()
        if (notes.isEmpty()) {
            Toast.makeText(this, "Tap + to add a note", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> { showAddDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddDialog() {
        val titleInput = EditText(this).apply {
            hint = "Title"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val contentInput = EditText(this).apply {
            hint = "Note"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 3
        }

        val container = LinearLayoutCompat2(this).apply {
            setPaddingDp(20)
            addView(titleInput)
            addView(contentInput)
        }

        AlertDialog.Builder(this)
            .setTitle("New note")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val t = titleInput.text.toString().trim()
                val c = contentInput.text.toString().trim()
                if (t.isEmpty() && c.isEmpty()) {
                    Toast.makeText(this, "Empty note not saved", Toast.LENGTH_SHORT).show()
                } else {
                    val note = Note(title = t, content = c)
                    notes.add(0, note)
                    adapter.notifyItemInserted(0)
                    rv.scrollToPosition(0)
                    saveNotes()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(note: Note) {
        val titleInput = EditText(this).apply {
            setText(note.title)
            hint = "Title"
        }
        val contentInput = EditText(this).apply {
            setText(note.content)
            hint = "Note"
            minLines = 3
        }

        val container = LinearLayoutCompat2(this).apply {
            setPaddingDp(20)
            addView(titleInput)
            addView(contentInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit note")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                note.title = titleInput.text.toString().trim()
                note.content = contentInput.text.toString().trim()
                adapter.notifyDataSetChanged()
                saveNotes()
            }
            .setNeutralButton("Share") { _, _ ->
                shareText("${note.title}\n\n${note.content}")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Delete note")
            .setMessage("This cannot be undone")
            .setPositiveButton("Delete") { _, _ ->
                val idx = notes.indexOfFirst { it.id == note.id }
                if (idx != -1) {
                    notes.removeAt(idx)
                    adapter.notifyItemRemoved(idx)
                    saveNotes()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNotes() {
        val arr = JSONArray()
        notes.forEach { n ->
            val o = JSONObject()
                .put("id", n.id)
                .put("title", n.title)
                .put("content", n.content)
                .put("timestamp", n.timestamp)
            arr.put(o)
        }
        prefs.edit().putString(KEY_NOTES, arr.toString()).apply()
    }

    private fun loadNotes() {
        val json = prefs.getString(KEY_NOTES, null) ?: return
        val arr = JSONArray(json)
        notes.clear()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            notes.add(
                Note(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    content = o.getString("content"),
                    timestamp = o.getLong("timestamp")
                )
            )
        }
        adapter.notifyDataSetChanged()
    }

    private fun shareText(text: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        startActivity(android.content.Intent.createChooser(intent, "Share note"))
    }
}

/** Tiny helper layout to avoid external libs */
import android.content.Context
import android.util.TypedValue
import android.widget.LinearLayout

class LinearLayoutCompat2(ctx: Context) : LinearLayout(ctx) {
    init { orientation = VERTICAL }
    fun setPaddingDp(all: Int) {
        val px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, all.toFloat(), resources.displayMetrics
        ).toInt()
        setPadding(px, px, px, px)
    }
}
