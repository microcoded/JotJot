package fail.enormous.jotjot

data class Jot(
        val type: String,
        var creation_date: String,
        var title: String,
        var content: String,
        var alert_time: Long)

/* open class Jot(creation_date: Long, title: String, content: String) {
    // Defines the class Jot, which will be used for any Jots within JotJot

    val creation_date: Long = 0
    val title: String = ""
    val content: String = ""

    // Return the current time
    fun saveTime(): Long {
       return Calendar.getInstance().timeInMillis
    }
}

class note(creation_date: Long, title: String, content: String, type: String): Jot(creation_date, title, content) {

}

class reminder(creation_date: Long, title: String, alert_time: Long, type: String): Jot(creation_date, title, alert_time.toString()) {

}

class list(creation_date: Long, title: String, content: String, type: String): Jot(creation_date, title, content) {

} */