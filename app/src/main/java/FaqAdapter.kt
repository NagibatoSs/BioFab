import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView       // ← ЭТО ВАЖНО!
import androidx.recyclerview.widget.RecyclerView
import com.example.biofab.R

class FaqAdapter(private val items: List<FaqItem>) :
    RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    private var expandedPosition = -1
    inner class FaqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val question = itemView.findViewById<TextView>(R.id.tvQuestion)

        val questionLayout = itemView.findViewById<LinearLayout>(R.id.ll_question)
        val answer = itemView.findViewById<TextView>(R.id.tvAnswer)
        val arrow = itemView.findViewById< ImageView>(R.id.ivArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.faq_item, parent, false)
        return FaqViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        val item = items[position]

        // только один раскрыт, остальные закрыты
        val isExpanded = position == expandedPosition
        holder.answer.visibility = if (isExpanded) View.VISIBLE else View.GONE

        holder.questionLayout.setOnClickListener {
            val previousExpanded = expandedPosition

            // если нажали на уже открытый → закрыть
            if (isExpanded) {
                expandedPosition = -1
            } else {
                expandedPosition = position
            }
            // анимация вращения против часовой стрелки
            val start = if (expandedPosition == position) 0f else 180f
            val end = if (expandedPosition == position) 180f else 0f

            holder.arrow.animate()
                .rotationBy(end - start)  // поворот на нужный угол
                .setDuration(200)
                .start()
            // обновить старый и новый элементы
            notifyItemChanged(previousExpanded)
            notifyItemChanged(expandedPosition)
        }
        holder.arrow.rotation = if (isExpanded) 0f else 180f
        holder.question.text = item.question
        holder.answer.text = item.answer
    }

    override fun getItemCount() = items.size
}