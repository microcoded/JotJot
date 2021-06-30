package fail.enormous.jotjot

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class RemindersRecyclerAdapter(private val context: Context, private val listRecyclerItem: List<Any>, private val cellClickListener: Reminders) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Setting values for all the TextViews
        val title: TextView = itemView.findViewById<View>(R.id.title) as TextView
        val alert_time: TextView = itemView.findViewById<View>(R.id.content) as TextView
        var creation_date: TextView = itemView.findViewById<View>(R.id.creation_date) as TextView
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        return when (i) {
            TYPE -> {
                val layoutView = LayoutInflater.from(viewGroup.context).inflate(
                    R.layout.recycler_item, viewGroup, false)
                ItemViewHolder(layoutView)
            }
            else -> {
                val layoutView = LayoutInflater.from(viewGroup.context).inflate(
                    R.layout.recycler_item, viewGroup, false)
                ItemViewHolder(layoutView)
            }
        }
    }


    @SuppressLint("StringFormatMatches", "SetTextI18n")
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {
        when (getItemViewType(i)) {
            TYPE -> {
                val itemViewHolder = viewHolder as ItemViewHolder
                val jotlist: Jot = listRecyclerItem[i] as Jot
                itemViewHolder.title.text = "${jotlist.title} (${jotlist.type})"
                itemViewHolder.alert_time.text = jotlist.content // Doing something hacky, using the content field to display alert time for compatability with formatting as string
                itemViewHolder.creation_date.text = jotlist.creation_date

                itemViewHolder.itemView.setOnClickListener {
                    cellClickListener.onCellClickListener(i)
                }
            }
            else -> {
                val itemViewHolder = viewHolder as ItemViewHolder
                val jotlist: Jot = listRecyclerItem[i] as Jot
                itemViewHolder.title.text = jotlist.title
                itemViewHolder.alert_time.text = jotlist.content
                itemViewHolder.creation_date.text = jotlist.creation_date

                itemViewHolder.itemView.setOnClickListener {
                    cellClickListener.onCellClickListener(i)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return listRecyclerItem.size
    }

    companion object {
        private const val TYPE = 1
    }

    interface CellClickListener {
        fun onCellClickListener(pos: Int)
    }

}
