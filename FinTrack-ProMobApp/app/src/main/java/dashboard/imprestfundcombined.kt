package com.example.aicomsapp.viewmodels.dashboard

import android.graphics.Color
import android.provider.ContactsContract.CommonDataKinds.Im
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.example.aicomsapp.R
import com.example.aicomsapp.viewmodels.response.ImprestFund
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImprestFundAdapterDashboard(
    private var imprestFundsList: List<ImprestFund>,
    private val onItemClick: (ImprestFund) -> Unit,
    private val userStatus: Int,
    private val onPictureClick: (ImprestFund) -> Unit,
    private val onPhotoSudahClick: (ImprestFund) -> Unit,
    private val onTransactionTypeChange: (ImprestFund, String) -> Unit,
    private val onStatusChangeClick: (ImprestFund) -> Unit// Pass the clicked item data
) : RecyclerView.Adapter<ImprestFundAdapterDashboard.ImprestFundViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImprestFundViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycle_view_imprest_admin, parent, false)
        return ImprestFundViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImprestFundViewHolder, position: Int) {
        val imprestFund = imprestFundsList[position]
        holder.bind(imprestFund, userStatus
        )

        // Set the click listener on the ViewHolder
        holder.itemView.setOnClickListener {
            onItemClick(imprestFund) // Pass the clicked item to the listener
        }
        holder.picture.setOnClickListener {
            onPictureClick(imprestFund) // Add this line to handle picture click
        }
        holder.photoSudah.setOnClickListener {
            onPhotoSudahClick(imprestFund)
        }
        holder.itemView.setOnLongClickListener {
            val newTransactionType = if (imprestFund.transactionType == "out") "in" else "out"
            onTransactionTypeChange(imprestFund, newTransactionType)
            true
        }
        holder.button.setOnClickListener {
            onStatusChangeClick(imprestFund)
        }

        val context = holder.itemView.context
        if (position == 0) {
            holder.itemView.background = AppCompatResources.getDrawable(context, R.drawable.rounder_top)
        } else {
            holder.itemView.background = AppCompatResources.getDrawable(context, R.drawable.no_rounded)
        }
    }
    fun updateData(newImprestFundsList: List<ImprestFund>) {
        imprestFundsList = newImprestFundsList
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int {
        return imprestFundsList.size
    }

    class ImprestFundViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userIdTextView: TextView = itemView.findViewById(R.id.textViewUserId)
        private val inputDateTextView: TextView = itemView.findViewById(R.id.textViewInputDate)
        private val transactionDateTextView: TextView = itemView.findViewById(R.id.textViewTransDate)
        private val amountTextView: TextView = itemView.findViewById(R.id.textViewAmount)
        public val picture: View = itemView.findViewById(R.id.imageViewPic)
        public val photoSudah: View = itemView.findViewById(R.id.imageViewphotosudah)
        private val sudah: View = itemView.findViewById(R.id.sudah)
        private val belum: View = itemView.findViewById(R.id.belum)
        public val button: View = itemView.findViewById(R.id.buttonUbahStatus)

        fun bind(imprestFund: ImprestFund, userStatus: Int) {
            userIdTextView.text = "User ID: ${imprestFund.name}"

            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            // Parse tanggal input
            val inputDate = inputDateFormat.parse(imprestFund.inputDate)
            val formattedInputDate = outputDateFormat.format(inputDate)
            inputDateTextView.text = "Tanggal Input: $formattedInputDate"

            // Parse tanggal transaksi
            val transactionDate = inputDateFormat.parse(imprestFund.transactionDate)
            val formattedTransactionDate = outputDateFormat.format(transactionDate)
            transactionDateTextView.text = "Tanggal Transaksi: $formattedTransactionDate"
            amountTextView.text = "Jumlah Uang: Rp ${imprestFund.amount}"
            Log.d("StatusCheck", "Status: ${imprestFund.status}")
            if (userStatus == 1) {
                photoSudah.visibility = View.GONE
            } else {
                photoSudah.visibility = View.VISIBLE
                button.visibility = View.GONE
            }
            when (imprestFund.status) {
                "done" -> {
                    sudah.visibility = View.VISIBLE

                    belum.visibility = View.GONE

                }

                "undone" -> {
                    belum.visibility = View.VISIBLE

                    sudah.visibility = View.GONE


                }
        }
    }
}
}
