package com.example.lostfound.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lostfound.R
import com.example.lostfound.databinding.ItemLostFoundBinding
import com.example.lostfound.model.Item

class ItemAdapter(

    // Card laporan ditekan
    private val onItemClick: (Item) -> Unit,

    // Tombol Bookmark ditekan
    private val onBookmarkClick: (Item) -> Unit

) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private val itemList =
        mutableListOf<Item>()

    /*
     * Berisi ID laporan yang sudah disimpan.
     */
    private val bookmarkedItemIds =
        mutableSetOf<String>()

    fun setItems(
        newItems: List<Item>
    ) {

        itemList.clear()
        itemList.addAll(newItems)

        notifyDataSetChanged()
    }

    /*
     * Memperbarui daftar ID Bookmark.
     */
    fun setBookmarkedItemIds(
        newIds: Set<String>
    ) {

        bookmarkedItemIds.clear()
        bookmarkedItemIds.addAll(newIds)

        notifyDataSetChanged()
    }

    inner class ItemViewHolder(
        private val binding: ItemLostFoundBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {

            val context =
                binding.root.context

            binding.tvItemTitle.text =
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
            displayBookmarkState(item)

            /*
             * Card ditekan untuk membuka Detail.
             */
            binding.root.setOnClickListener {
                onItemClick(item)
            }

            /*
             * Tombol Bookmark ditekan.
             */
            binding.btnBookmark.setOnClickListener {
                onBookmarkClick(item)
            }
        }

        private fun displayReportType(
            item: Item
        ) {

            val context =
                binding.root.context

            if (
                item.reportType ==
                REPORT_TYPE_FOUND
            ) {

                binding.tvReportType.text =
                    context.getString(
                        R.string.report_found
                    )

                binding.tvReportType
                    .setBackgroundResource(
                        R.drawable.bg_badge_found
                    )

            } else {

                binding.tvReportType.text =
                    context.getString(
                        R.string.report_lost
                    )

                binding.tvReportType
                    .setBackgroundResource(
                        R.drawable.bg_badge_lost
                    )
            }
        }

        private fun displayStatus(
            item: Item
        ) {

            val context =
                binding.root.context

            if (
                item.status ==
                STATUS_RETURNED
            ) {

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
            }
        }

        private fun displayImage(
            item: Item
        ) {

            Glide.with(binding.ivItem)
                .clear(binding.ivItem)

            if (item.imageUrl.isNotEmpty()) {

                Glide.with(binding.ivItem)
                    .load(item.imageUrl)
                    .placeholder(
                        android.R.drawable.ic_menu_gallery
                    )
                    .error(
                        android.R.drawable.ic_menu_report_image
                    )
                    .centerCrop()
                    .into(binding.ivItem)

            } else {

                binding.ivItem.setImageResource(
                    android.R.drawable.ic_menu_gallery
                )
            }
        }

        private fun displayBookmarkState(
            item: Item
        ) {

            val context =
                binding.root.context

            val isBookmarked =
                bookmarkedItemIds.contains(
                    item.id
                )

            if (isBookmarked) {

                binding.btnBookmark.text =
                    context.getString(
                        R.string.saved_bookmark
                    )

                binding.btnBookmark
                    .backgroundTintList =
                    ContextCompat.getColorStateList(
                        context,
                        R.color.lf_secondary
                    )

            } else {

                binding.btnBookmark.text =
                    context.getString(
                        R.string.save_bookmark
                    )

                binding.btnBookmark
                    .backgroundTintList =
                    ContextCompat.getColorStateList(
                        context,
                        R.color.lf_primary
                    )
            }

            /*
             * Laporan tanpa ID tidak boleh disimpan.
             */
            binding.btnBookmark.isEnabled =
                item.id.isNotEmpty()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemViewHolder {

        val binding =
            ItemLostFoundBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int
    ) {

        holder.bind(
            itemList[position]
        )
    }

    override fun getItemCount(): Int {

        return itemList.size
    }

    companion object {

        private const val REPORT_TYPE_FOUND =
            "FOUND"

        private const val STATUS_RETURNED =
            "RETURNED"
    }
}