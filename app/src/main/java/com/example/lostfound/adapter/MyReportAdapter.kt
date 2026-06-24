package com.example.lostfound.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lostfound.R
import com.example.lostfound.databinding.ItemMyReportBinding
import com.example.lostfound.model.Item

class MyReportAdapter(

    // Membuka halaman detail
    private val onDetailClick: (Item) -> Unit,

    // Membuka halaman edit
    private val onEditClick: (Item) -> Unit,

    // Mengubah status laporan
    private val onStatusClick: (Item) -> Unit,

    // Menghapus laporan
    private val onDeleteClick: (Item) -> Unit

) : RecyclerView.Adapter<MyReportAdapter.MyReportViewHolder>() {

    private val reports = mutableListOf<Item>()

    fun setReports(newReports: List<Item>) {

        reports.clear()
        reports.addAll(newReports)

        notifyDataSetChanged()
    }

    inner class MyReportViewHolder(
        private val binding: ItemMyReportBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {

            val context = binding.root.context

            binding.tvTitle.text =
                item.title.ifEmpty {
                    context.getString(
                        R.string.value_not_available
                    )
                }

            binding.tvCategory.text =
                context.getString(
                    R.string.item_category_value,
                    item.category
                )

            binding.tvLocation.text =
                context.getString(
                    R.string.item_location_value,
                    item.location
                )

            binding.tvDate.text =
                context.getString(
                    R.string.item_date_value,
                    item.eventDate
                )

            displayReportType(item)
            displayStatus(item)
            displayImage(item)

            binding.btnDetail.setOnClickListener {
                onDetailClick(item)
            }

            binding.btnEdit.setOnClickListener {
                onEditClick(item)
            }

            binding.btnStatus.setOnClickListener {
                onStatusClick(item)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }

        private fun displayReportType(item: Item) {

            val context = binding.root.context

            if (item.reportType == REPORT_TYPE_FOUND) {

                binding.tvReportType.text =
                    context.getString(
                        R.string.report_found
                    )

                binding.tvReportType.setBackgroundResource(
                    R.drawable.bg_badge_found
                )

            } else {

                binding.tvReportType.text =
                    context.getString(
                        R.string.report_lost
                    )

                binding.tvReportType.setBackgroundResource(
                    R.drawable.bg_badge_lost
                )
            }
        }

        private fun displayStatus(item: Item) {

            val context = binding.root.context

            if (item.status == STATUS_RETURNED) {

                binding.tvStatus.text =
                    context.getString(
                        R.string.status_returned
                    )

                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.lf_status_returned
                    )
                )

                binding.btnStatus.text =
                    context.getString(
                        R.string.reopen_report
                    )

            } else {

                binding.tvStatus.text =
                    context.getString(
                        R.string.status_open
                    )

                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.lf_status_open
                    )
                )

                binding.btnStatus.text =
                    context.getString(
                        R.string.mark_returned
                    )
            }
        }

        private fun displayImage(item: Item) {

            Glide.with(binding.ivReportImage)
                .clear(binding.ivReportImage)

            if (item.imageUrl.isNotEmpty()) {

                Glide.with(binding.ivReportImage)
                    .load(item.imageUrl)
                    .placeholder(
                        android.R.drawable.ic_menu_gallery
                    )
                    .error(
                        android.R.drawable.ic_menu_report_image
                    )
                    .centerCrop()
                    .into(binding.ivReportImage)

            } else {

                binding.ivReportImage.setImageResource(
                    android.R.drawable.ic_menu_gallery
                )
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyReportViewHolder {

        val binding = ItemMyReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MyReportViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MyReportViewHolder,
        position: Int
    ) {
        holder.bind(reports[position])
    }

    override fun getItemCount(): Int {
        return reports.size
    }

    companion object {

        private const val REPORT_TYPE_FOUND =
            "FOUND"

        private const val STATUS_RETURNED =
            "RETURNED"
    }
}