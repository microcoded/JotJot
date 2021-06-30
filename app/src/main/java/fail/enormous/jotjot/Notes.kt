package fail.enormous.jotjot

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import java.io.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Notes : AppCompatActivity() {
    @SuppressLint("StringFormatMatches")

    private var viewItems: MutableList<Any> = ArrayList()
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var layoutManager: RecyclerView.LayoutManager? = null

    @SuppressLint("StringFormatMatches")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        // Total number of notes
        val notes_tv: TextView = findViewById(R.id.notes_textview)
        val notes_number: Int = totalNotes()
        notes_tv.text = getString(R.string.notecount, notes_number)

        // Set up recycler view
        setupRecycler()

    }

    @SuppressLint("DefaultLocale")
    private fun totalNotes(): Int {
        // Read the JSON file
        var noteCounter = 0
        val jsonDataString = readJSONDataFromFile()
        val jsonArray = JSONArray(jsonDataString)
        for (i in 0 until jsonArray.length()) {
            // For each iteration being a Note, add 1 to counter
            if (jsonArray.getJSONObject(i).getString("type").toLowerCase() == "note") { noteCounter += 1 }
        }
        return noteCounter
    }

    private fun setupRecycler() {
        val mRecyclerView: RecyclerView = findViewById<View>(R.id.notes_recyclerview) as RecyclerView

        // Using a linear layout manager
        layoutManager = LinearLayoutManager(this)
        mRecyclerView.layoutManager = layoutManager

        // Specifying adapter
        val mAdapter = NotesRecyclerAdapter(this, viewItems, this)
        mRecyclerView.adapter = mAdapter
        addItemsFromJSON()
        refreshRecycler()
    }

    @SuppressLint("DefaultLocale")
    private fun addItemsFromJSON() {
        val jsonDataString = readJSONDataFromFile()
        Log.d("a", jsonDataString)
        val jsonArray = JSONArray(jsonDataString)

        for (i in 0 until jsonArray.length()) {
            if (jsonArray.getJSONObject(i).getString("type").toLowerCase() == "note") {
                val itemObj = jsonArray.getJSONObject(i)
                val creation_date = itemObj.getLong("creation_date")
                val title = itemObj.getString("title")
                val content = itemObj.getString("content")
                val type = itemObj.getString("type").capitalize()
                val jotlist = Jot(type, dateFormatter(creation_date), title, content, 0)
                viewItems.add(jotlist)
            }
        }
    }

    private fun readJSONDataFromFile(): String {
        val inputStream: InputStream? = null
        val builder = StringBuilder()
        try {
            val fileName = "jotlist.json"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (storageDir != null) {
                // Make the directory if it doesn't exist
                if (!storageDir.exists()){
                    storageDir.mkdir()
                }
            }
            var jsonString: String? = null
            val bufferedReader = File(storageDir, fileName).bufferedReader()

            // Reading data per line, appending it to builder
            while (bufferedReader.readLine().also { jsonString = it } != null) {
                // Appending string information from the file to the builder
                builder.append(jsonString)
                Log.d("while loop", jsonString.toString())
            }
        }
        finally {
            // Close file once finished
            inputStream?.close()
            Log.d("string builder", String(builder))
        }
        // Return the string
        Log.d("string builder", String(builder))
        return String(builder)
    }

    private fun dateFormatter(date: Long): String {
        val myDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(date), TimeZone.getDefault().toZoneId())
        val myFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return myDate.format(myFormat)
    }

    private fun refreshRecycler() {
        viewItems.clear()
        mAdapter?.notifyDataSetChanged()
        mAdapter = NotesRecyclerAdapter(this, viewItems, this)
        mRecyclerView?.adapter = mAdapter
        addItemsFromJSON()
    }

    fun onCellClickListener(i: Int) {
        Log.d("Recycler, tapped", i.toString())
        val jsonFileString = getJsonDataFromAsset(applicationContext, "jotlist.json")
        val arrayJotType = object : TypeToken<Array<Jot>>() {}.type
        val jots: Array<Jot> = Gson().fromJson(jsonFileString, arrayJotType)
        var k = 0

        val items = Array<Jot>(jots.size){ Jot("", "", "", "", 0) }
        for (j in jots.indices) {
            if (jots[j].type == "note") {
                items[k] = jots[j]
                // Something hacky, storing the position of the original Jot as read from the file [eg, 4] into its content (as these are not used for reminders)
                items[k].content = j.toString()
                k += 1
            }
        }
        optionDialogue(i, items)
    }

    private fun optionDialogue(pos: Int, items: Array<Jot>) {
        var chosen = 0
        val listItems = arrayOf(getString(R.string.edit), getString(R.string.delete))
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(items[pos].title)
        val checkedItem = -1

        builder.setSingleChoiceItems(
            listItems,
            checkedItem,
            DialogInterface.OnClickListener { dialog, which ->
                chosen = which
                if (chosen == 0) { editNote(pos) }
                if (chosen == 1) { deleteNote(pos) }
                dialog.dismiss()
            }
        )

        /* builder.setPositiveButton(
            R.string.cancel,
            DialogInterface.OnClickListener {dialog, which ->
                dialog.dismiss()

            }
        ) */

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    fun editNote(i: Int){
        val intent = Intent(this, EditNote::class.java)
        intent.putExtra("selectedItem", i)
        startActivity(intent)
    }

    private fun deleteNote(pos: Int) {
        val jsonFileString = getJsonDataFromAsset(applicationContext, "jotlist.json")
        val arrayJotType = object : TypeToken<Array<Jot>>() {}.type
        val jots: Array<Jot> = Gson().fromJson(jsonFileString, arrayJotType)

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

        val jotsRemoved = remove(jots, notes[pos].alert_time.toInt())
        saveJSON(GsonBuilder().setPrettyPrinting().create().toJson(jotsRemoved))

        overridePendingTransition(0, 0)
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)

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

    private fun remove(arr: Array<Jot>, index: Int): Array<Jot> {
        if (index < 0 || index >= arr.size) {
            return arr
        }
        val result = arr.toMutableList()
        result.removeAt(index)
        return result.toTypedArray()
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

    fun newNote(view: View) {
        startActivity(Intent(this, AddNote::class.java), ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java), ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

}