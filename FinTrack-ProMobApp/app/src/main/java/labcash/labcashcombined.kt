package com.example.aicomsapp.viewmodels.labcash


import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.example.aicomsapp.R
import com.example.aicomsapp.viewmodels.response.ImprestFund
import com.example.aicomsapp.viewmodels.response.LabCash
import java.text.SimpleDateFormat
import java.util.Locale

class LabCashAdapter(
    private var labCashList: List<LabCash>,
    private val onItemClick: (LabCash) -> Unit,
    private val onPictureClick: (LabCash) -> Unit,
    private val onPhotoSudahClick: (LabCash) -> Unit,
    private val onTransactionTypeChange: (LabCash, String) -> Unit,
    private val onStatusChangeClick: (LabCash) -> Unit
) : RecyclerView.Adapter<LabCashAdapter.LabCashViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabCashViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycle_view_labcash, parent, false)
        return LabCashViewHolder(view)
    }

    override fun onBindViewHolder(holder: LabCashViewHolder, position: Int) {
        val labCash = labCashList[position]
        holder.bind(labCash)

        // Set the click listener on the ViewHolder
        holder.itemView.setOnClickListener {
            onItemClick(labCash) // Pass the clicked item to the listener
        }
        holder.picture.setOnClickListener {
            onPictureClick(labCash) // Add this line to handle picture click
        }
        holder.photoSudah.setOnClickListener {
            onPhotoSudahClick(labCash)
        }
        holder.itemView.setOnLongClickListener {
            val newTransactionType = if (labCash.transactionType == "out") "in" else "out"
            onTransactionTypeChange(labCash, newTransactionType)
            true
        }
        holder.button.setOnClickListener {
            onStatusChangeClick(labCash)
        }
        val context = holder.itemView.context
        if (position == 0) {
            holder.itemView.background =
                AppCompatResources.getDrawable(context, R.drawable.rounder_top)
        } else {
            holder.itemView.background =
                AppCompatResources.getDrawable(context, R.drawable.no_rounded)
        }
    }
    fun updateData(newLabCashList: List<LabCash>) {
        labCashList = newLabCashList
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return labCashList.size
    }


    class LabCashViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userIdTextView: TextView = itemView.findViewById(R.id.textViewUserId)
        private val inputDateTextView: TextView = itemView.findViewById(R.id.textViewInputDate)
        private val amountTextView: TextView = itemView.findViewById(R.id.textViewAmount)
        public val picture: View = itemView.findViewById(R.id.imageViewPic)
        public val photoSudah: View = itemView.findViewById(R.id.imageViewphotosudah)
        private val masuk: View = itemView.findViewById(R.id.masuk)
        private val keluar: View = itemView.findViewById(R.id.keluar)
        private val donasi: View = itemView.findViewById(R.id.donasi)
        public val button: View = itemView.findViewById(R.id.buttonUbahStatus)

        fun bind(labCash: LabCash) {
            userIdTextView.text = "User ID: ${labCash.name}"

            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            // Parse tanggal input
            val inputDate = inputDateFormat.parse(labCash.inputDate)
            val formattedInputDate = outputDateFormat.format(inputDate)
            inputDateTextView.text = "Tanggal Input: $formattedInputDate"
            amountTextView.text = "Jumlah Uang: Rp ${labCash.amount}"
            when (labCash.transactionType) {
                "in" -> {
                    masuk.visibility = View.VISIBLE

                    keluar.visibility = View.GONE

                    donasi.visibility = View.GONE

                    button.visibility = View.VISIBLE
                }

                "out" -> {
                    keluar.visibility = View.VISIBLE

                    masuk.visibility = View.GONE

                    donasi.visibility = View.GONE

                    button.visibility = View.VISIBLE

                }

                "donation" -> {
                    donasi.visibility = View.VISIBLE

                    masuk.visibility = View.GONE

                    keluar.visibility = View.GONE

                    button.visibility = View.GONE


                }
            }
        }
    }
    fun updateTransactionType(labCash: LabCash, newTransactionType: String) {
        onTransactionTypeChange(labCash, newTransactionType) // Panggil callback saat transaksi berubah
    }
}
