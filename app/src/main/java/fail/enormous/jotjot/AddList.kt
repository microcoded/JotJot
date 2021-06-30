package fail.enormous.jotjot

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.transition.AutoTransition
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.*

class AddList : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_list)
    }

    // Close the keyboard when clicking out
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    // When the done button is pressed
    fun addList(view: View) {
        val titleInput = this.findViewById<EditText>(R.id.title_editText)

        // Get the text from each field
        val titleText = titleInput.text.toString()

        addListToFile(titleText)
        "Lists".goto()
    }

    private fun addListToFile(title: String) {
        val saveTime = System.currentTimeMillis().toString()
        val jotList = mutableListOf(
            Jot("list", saveTime, title, "", 0)
        )

        val fileName = "jotlist.json"

        // Read data from existing file if it exists
        if (File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName).exists()) {

            // Variables for Gson initialisation
            val jsonFileString = getJsonDataFromAsset(applicationContext, fileName)
            val arrayJotType = object : TypeToken<Array<Jot>>() {}.type

            // Convert JSON into an array
            val jots: Array<Jot> = Gson().fromJson(jsonFileString, arrayJotType)
            for (i in jots.indices) {
                val type = jots[i].type
                val creation_date = jots[i].creation_date
                val title = jots[i].title
                val content = jots[i].content
                val alert_time = jots[i].alert_time

                // Add onto jotList
                jotList.add(Jot(type, creation_date, title, content, alert_time))
            }
        }

        saveJSON(GsonBuilder().setPrettyPrinting().create().toJson(jotList))

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

    private fun String.goto() {
        if (this == "Lists") {
            startActivity(Intent(this@AddList, Lists::class.java), ActivityOptions.makeSceneTransitionAnimation(this@AddList).toBundle())
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}

