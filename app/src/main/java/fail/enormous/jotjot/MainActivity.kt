package fail.enormous.jotjot

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.transition.AutoTransition
import android.util.Log
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import org.json.JSONArray
import org.json.JSONException
import java.io.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class MainActivity : AppCompatActivity(), RecyclerAdapter.CellClickListener {
    @SuppressLint("StringFormatMatches") // I know what I'm doing with inserting variables into Strings!

    private var viewItems: MutableList<Any> = ArrayList()
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var layoutManager: RecyclerView.LayoutManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setAnimation()

        // This app won't function without storage permissions.
        requestStoragePermission()

        // When something in the gridLayout is clicked, get its position
        val gl: GridLayout = findViewById(R.id.main_gridlayout)
        val childCount: Int = gl.childCount
        for (i in 0 until childCount) {
            gl.getChildAt(i).setOnClickListener {
                gridClicked(i)
            }
        }

        // Set up the RecyclerView
        setupRecycler()

        // Show total number of Jots
        val jots_tv: TextView = findViewById(R.id.jots_textview)
        val jots_number: Int = totalJots()
        jots_tv.text = getString(R.string.jots, jots_number)
    }

    private fun setAnimation() {
        val autoT = AutoTransition()
        window.exitTransition = autoT
        window.enterTransition = autoT
    }

    private fun setupRecycler() {
        val mRecyclerView: RecyclerView = findViewById<View>(R.id.recents_recyclerview) as RecyclerView

        // Using a linear layout manager
        layoutManager = LinearLayoutManager(this)
        mRecyclerView.layoutManager = layoutManager

        // Specifying adapter
        val mAdapter = RecyclerAdapter(this, viewItems, this)
        mRecyclerView.adapter = mAdapter
        addItemsFromJSON()
        refreshRecycler()
    }

    @SuppressLint("DefaultLocale")
    private fun addItemsFromJSON() {
        var content: String
        try {
            var loopSize = 0 // Initialising loopSize variable
            val jsonDataString = readJSONDataFromFile()
            Log.d("a", jsonDataString)
            val jsonArray = JSONArray(jsonDataString)

            // Display maximum of 5 recent Jots
            loopSize = if (jsonArray.length() <= 5) {
                jsonArray.length()
            } else {
                5
            }

            for (i in 0 until loopSize) {
                val itemObj = jsonArray.getJSONObject(i)
                val creation_date = itemObj.getLong("creation_date")
                val title = itemObj.getString("title")
                val type = itemObj.getString("type").capitalize()
                if (type == "Reminder") {
                    content = getString(R.string.notify_at, dateFormatter(itemObj.getString("alert_time").toLong()))
                } else {
                    content = itemObj.getString("content")
                }
                val jotlist = Jot(type, dateFormatter(creation_date), title, content, 0)
                viewItems.add(jotlist)
            }
        }

        // Error handling and debugging
        catch (e: JSONException) {
            Log.d(TAG, "addItemsFromJSON: ", e)
        }
        catch (e: IOException) {
            Log.d(TAG, "addItemsFromJSON: ", e)
        }

    }

    private fun dateFormatter(date: Long): String {
        val myDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(date), TimeZone.getDefault().toZoneId())
        val myFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return myDate.format(myFormat)
    }

    // Tags for error handling in addItemsFromJSON()
    companion object {
        private const val TAG = "MainActivity"
    }

    private fun readJSONDataFromFile(): String {
        val inputStream: InputStream? = null
        val builder = StringBuilder()

        // If the file doesn't exist, make a blank one so JotJot doesn't freak out and freeze
        if (!File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "jotlist.json").exists()) {
            val output: Writer
            val file = createFile()
            output = BufferedWriter(FileWriter(file))
            output.write("[]")
            output.close()
        }

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

    private fun requestStoragePermission() {
        // Dexter library implementation for requesting permissions - https://github.com/Karumi/Dexter

        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_DOCUMENTS
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) { /* ... */
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest?>?, token: PermissionToken?) {
                        Toast.makeText(applicationContext, R.string.permissions_none, Toast.LENGTH_SHORT).show()
                    }
                }).check()
    }

    private fun refreshRecycler() {
        viewItems.clear()
        mAdapter?.notifyDataSetChanged()
        mAdapter = RecyclerAdapter(this, viewItems, this)
        mRecyclerView?.adapter = mAdapter
        addItemsFromJSON()
    }


    private fun totalJots(): Int {
        // Read the JSON file if it exists
        if (File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "jotlist.json").exists()) {
            var jotCounter = 0
            val jsonDataString = readJSONDataFromFile()
            val jsonArray = JSONArray(jsonDataString)
            for (i in 0 until jsonArray.length()) {
                // For each iteration, add 1 Jot to counter
                jotCounter += 1
            }
            return jotCounter
        } else {
            return 0
        }
    }

    private fun gridClicked(pos: Int) {
        if (pos == 0) { notes() }
        if (pos == 1) { reminders() }
        if (pos == 2) { list() }
    }

    private fun notes() {
        startActivity(Intent(this, Notes::class.java))
    }

    private fun reminders() {
        startActivity(Intent(this, Reminders::class.java))
    }

    private fun list() {
        startActivity(Intent(this, Lists::class.java))
    }

    fun newNote(view: View) {
        startActivity(Intent(this, AddNote::class.java), ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    fun newReminder(view: View) {
        startActivity(Intent(this, AddReminder::class.java), ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    fun newList(view: View) {
        startActivity(Intent(this, AddList::class.java), ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    // When an item is clicked in the RecyclerView
    override fun onCellClickListener(pos: Int) {
        Log.d("pos", pos.toString())
        val jsonDataString = readJSONDataFromFile()
        val jsonArray = JSONArray(jsonDataString)
        val type = jsonArray.getJSONObject(pos).getString("type").toLowerCase(Locale.ROOT)

        if (type == "note") { notes() }
        if (type == "reminder") { reminders() }
        if (type == "list") { list() }
    }

    override fun onBackPressed() {
        // Exit to home screen
        val gohome = Intent(Intent.ACTION_MAIN)
        gohome.addCategory(Intent.CATEGORY_HOME)
        gohome.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(gohome)
    }

    fun share(view: View) {
        val jsonFileString = getJsonDataFromAsset(applicationContext, "jotlist.json")
        val arrayJotType = object : TypeToken<Array<Jot>>() {}.type
        val jots: Array<Jot> = Gson().fromJson(jsonFileString, arrayJotType)
        val content: String = GsonBuilder().setPrettyPrinting().create().toJson(jots)

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, content)
        sendIntent.type = "text/json"
        startActivity(Intent.createChooser(sendIntent, getString(R.string.export)))
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

}
