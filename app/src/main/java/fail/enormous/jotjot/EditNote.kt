package fail.enormous.jotjot

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import java.io.*

class EditNote : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)
        // Getting selectedItem from Notes.kt, with default value of -1 as it is impossible.
        val selectedItem = intent.getIntExtra("selectedItem", -1)

        val notes = loadContent(selectedItem)

        val doneButton = findViewById<Button>(R.id.done_button)
        doneButton.setOnClickListener { saveNote(selectedItem, notes) }
    }

    // Close the keyboard when clicking out
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun loadContent(i: Int): Array<Jot> {
        val jsonFileString = getJsonDataFromAsset(applicationContext, "jotlist.json")
        val arrayJotType = object : TypeToken<Array<Jot>>() {}.type

        // Convert JSON into an array
        val jots: Array<Jot> = Gson().fromJson(jsonFileString, arrayJotType)
        /* val type = jots[i].type
        val alert_time = jots[i].alert_time
        val creation_date = jots[i].creation_date */

        // Making an array of just notes, so we know which number item we're using
        val notes = Array<Jot>(jots.size){ Jot("", "", "", "", 0) }
        var k = 0
        for (j in jots.indices) {
            if (jots[j].type == "note") {
                notes[k] = jots[j]
                // Something hacky, storing the position of the original Jot as read from the file [eg, 4] into it's alert time (as these are only used for reminders)
                notes[k].alert_time = j.toLong()
                k += 1
            }
        }

        val title = notes[i].title
        val content = notes[i].content
        val titleText: EditText = findViewById(R.id.title_editText)
        val contentText: EditText = findViewById(R.id.content_editText)


        // Place the data from the item into the EditText
        titleText.setText(title)
        contentText.setText(content)
        return notes
    }

    private fun getJsonDataFromAsset(context: Context, fileName: String): String? {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val jsonString: String
        try {
            jsonString = File(storageDir, fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }
        return jsonString
    }

    fun saveNote(i: Int, notes: Array<Jot>) {
        // Text inputs
        val titleInput = this.findViewById<EditText>(R.id.title_editText)
        val contentInput = this.findViewById<EditText>(R.id.content_editText)

        // Get the text from each field
        val titleText = titleInput.text.toString()
        val contentText = contentInput.text.toString()

        editNoteToFile(titleText, contentText, i, notes)
        startActivity(Intent(this, Notes::class.java))
    }

    private fun editNoteToFile(title: String, content: String, selectedItem: Int, notes: Array<Jot>) {
        val fileName = "jotlist.json"
        // Variables for Gson initialisation
        val jsonFileString = getJsonDataFromAsset(applicationContext, fileName)
        val arrayJotType = object : TypeToken<Array<Jot>>() {}.type

        // Convert JSON into an array
        val jots: Array<Jot> = Gson().fromJson(jsonFileString, arrayJotType)
        val pos = notes[selectedItem].alert_time.toInt()

        // Adding the new titles
        jots[pos].title = title
        jots[pos].content = content
        // Adjusting the date to the last edited time
        jots[pos].creation_date = System.currentTimeMillis().toString()

        saveJSON(GsonBuilder().setPrettyPrinting().create().toJson(jots))
    }

    private fun saveJSON(jsonString: String) {
        val output: Writer
        val file = createFile()
        output = BufferedWriter(FileWriter(file))
        output.write(jsonString)
        output.close()
    }

    private fun createFile(): File {
        val fileName = "jotlist.json"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (storageDir != null) {
            if (!storageDir.exists()){
                // Make folder if not found
                storageDir.mkdir()
            }
        }

        return File(storageDir, fileName)
    }
}