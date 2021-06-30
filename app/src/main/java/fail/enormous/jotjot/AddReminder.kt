package fail.enormous.jotjot

import android.annotation.SuppressLint
import android.app.*
import android.app.Notification.CATEGORY_REMINDER
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class AddReminder : AppCompatActivity() {
    var mDate: Long = 0 // Epic global variable which we'll use to store the date selected by the user
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContentView(R.layout.activity_add_reminder)
        val tv: TextView = findViewById<TextView>(R.id.alert_time_text)
        tv.text = getString(R.string.alert_text, "")
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.reminders)
        val descriptionText = getString(R.string.reminders)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("reminder", name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Close the keyboard when clicking out
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    fun setAlarm(time: Long, content: String) {
        val remindersIntent: Intent = Intent(getApplicationContext(), Reminders::class.java)
        val pi: PendingIntent = PendingIntent.getActivity(this, 0, remindersIntent, 0)
        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, _a: Intent) {
                /*  ok : so, here is where we do magical things with our alarm after it fires
                    HOWEVER, AlarmManager may actually pause alarms in order to conserve on battery or for other, higher
                    priority actions. Sometimes it might even fire alarms a few seconds late or early! It's not consistent
                    by design in the name of saving battery.
                */

                val builder = NotificationCompat.Builder(this@AddReminder, "reminder")
                    .setSmallIcon(R.drawable.ic_bell_black)
                    .setContentTitle("Reminder")
                    .setContentText(content) // Use content from earlier, the reminder name
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                // ✨Lambda Function✨
                with(NotificationManagerCompat.from(this@AddReminder)) {
                    notify(System.currentTimeMillis().toInt(), builder.build())
                }

                Log.d("AlarmManager(message)", content)
                context.unregisterReceiver(this) // this == BroadcastReceiver, not Activity
            }
        }
        this.registerReceiver(receiver, IntentFilter("fail.enormous.jotjot.reminderAction"))
        val pintent = PendingIntent.getBroadcast(this, 0, Intent("fail.enormous.jotjot.reminderAction"), 0)
        val manager = this.getSystemService(ALARM_SERVICE) as AlarmManager
        Log.d("AlarmManager(system_time)", System.currentTimeMillis().toString())
        Log.d("AlarmManager(time)", time.toString())
        manager[AlarmManager.RTC_WAKEUP, time] = pintent
    }

    // When the done button is pressed
    fun addReminder(view: View) {
        val titleInput = this.findViewById<EditText>(R.id.title_editText)
        val titleText = titleInput.text.toString()
        // Please select a date in the past, don't be silly
        if (System.currentTimeMillis() < mDate) {
            addReminderToFile(titleText, mDate)
            "Reminders".goto()
            setAlarm(mDate, titleText)
        }
        else {
            Toast.makeText(applicationContext, getString(R.string.future_date_please), Toast.LENGTH_LONG).show()
        }
    }

    private fun addReminderToFile(title: String, alert_time: Long) {
        val saveTime = System.currentTimeMillis().toString()
        val jotList = mutableListOf(
            Jot("reminder", saveTime, title, "", alert_time)
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
        if (this == "Reminders") {
            startActivity(Intent(this@AddReminder, Reminders::class.java), ActivityOptions.makeSceneTransitionAnimation(this@AddReminder).toBundle())
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    @SuppressLint("SetTextI18n")
    fun pickerDate(view: View) {
        val c: Calendar = Calendar.getInstance()
        val mYear = c.get(Calendar.YEAR)
        val mMonth = c.get(Calendar.MONTH)
        val mDay = c.get(Calendar.DAY_OF_MONTH)


        val datePickerDialog = DatePickerDialog(this,
            {view, year, monthOfYear, dayOfMonth -> pickerTime(year, monthOfYear, dayOfMonth)},
            mYear,
            mMonth,
            mDay)
        datePickerDialog.show()
    }

    private fun pickerTime(year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val c = Calendar.getInstance()
        val mHour = c[Calendar.HOUR_OF_DAY]
        val mMinute = c[Calendar.MINUTE]

        val timePickerDialog = TimePickerDialog(this,
            { view, hourOfDay, minute -> setDate(year, monthOfYear, dayOfMonth, hourOfDay, minute) },
            mHour,
            mMinute,
            false)
        timePickerDialog.show()
    }

    private fun setDate(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val tv: TextView = findViewById<TextView>(R.id.alert_time_text)
        val dateTime: LocalDateTime = LocalDateTime.of(year, month + 1, day, hour, minute)
        /*  Why is there a discrepancy between LocalDateTime and the way DatePicker
            grabs months? It was consistently a month behind every single time, hence `month + 1` */
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        tv.text = getString(R.string.alert_text, dateTime.format(formatter))
        mDate = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        Log.d("Selected date to UNIX", mDate.toString())
        // Display a warning if the date is in the past
        if (mDate < System.currentTimeMillis()) { Toast.makeText(applicationContext, getString(R.string.future_date_please), Toast.LENGTH_LONG).show() }
        return mDate
    }
}

