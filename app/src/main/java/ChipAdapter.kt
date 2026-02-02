import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.biofab.R
import com.google.android.material.chip.Chip

class ChipAdapter(
    private val items: List<String>,
    private val onChipSelected: (String) -> Unit
) : RecyclerView.Adapter<ChipAdapter.ChipViewHolder>() {

    private var selectedPosition = -1

    inner class ChipViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chip, parent, false) as Chip
        return ChipViewHolder(chip)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val item = items[position]
        holder.chip.text = item

        // состояние выбора
        holder.chip.isChecked = (position == selectedPosition)
        holder.chip.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

            val previous = selectedPosition

            selectedPosition = if (pos == selectedPosition) {
                -1              //повторный клик → снять checked
            } else {
                pos
            }

            if (previous != -1) notifyItemChanged(previous)
            if (selectedPosition != -1) notifyItemChanged(selectedPosition)

            onChipSelected(
                if (selectedPosition != -1) items[selectedPosition] else ""
            )
        }
    }

    override fun getItemCount(): Int = items.size
}