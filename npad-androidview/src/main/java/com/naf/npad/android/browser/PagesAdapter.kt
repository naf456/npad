package com.naf.npad.android.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.naf.npad.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException

class PagesAdapter(
    private val context: Context,
    pages: List<com.naf.npad.android.data.PageInfo>,
    private val thumbnailGetter: ThumbnailGetter,
    private val onLateLoadedListener: OnLateLoadedListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEWTYPE_WELCOME = 0
        const val VIEWTYPE_PAGEITEM = 1
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    class WelcomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class PageItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var rootView : View = itemView.findViewById(R.id.pageItem_root)
        private var titleTextView : TextView = itemView.findViewById(R.id.pageItem_title)
        private var detailTextView : TextView = itemView.findViewById(R.id.pageItem_detail)
        var thumbnailImageView : ImageView = itemView.findViewById(R.id.pageItem_thumbnail)
        private var loadingIndicator : ProgressBar = itemView.findViewById(R.id.pageItem_loading_indicator)
        private var accentShade : View = itemView.findViewById(R.id.pageItem_accent_shade)
        private var separator : View = itemView.findViewById(R.id.pageItem_seperator)

        var title: String?
            get() = titleTextView.text.toString()
            set(value) {
                if (value.isNullOrEmpty()) {
                    titleTextView.text = "[Untitled]"
                    titleTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                } else {
                    titleTextView.text = value
                    titleTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                }
            }

        var detail : String
            get() = detailTextView.text.toString()
            set(value) {
                detailTextView.text = value
            }

        var thumbnail : Bitmap?
            get() = thumbnailImageView.drawable.toBitmap()
            set(value){
                thumbnailImageView.setImageBitmap(value)
            }

        var isLoading : Boolean = loadingIndicator.visibility == View.VISIBLE
            set(value) {
                loadingIndicator.visibility = if (value) { View.VISIBLE } else { View.GONE }
                field = value
            }

        init {
            isLoading = false
        }

        val position get() = Rect(rootView.left, rootView.top, rootView.right, rootView.bottom)

        fun setTransitionId(id: String){
            ViewCompat.setTransitionName(thumbnailImageView, "$id-background")
            ViewCompat.setTransitionName(titleTextView, "$id-title")
        }

        fun setColoring(primary: Int, darkPrimary: Int) {
            titleTextView.setTextColor(primary)
            detailTextView.setTextColor(primary)
            accentShade.background.setTint(darkPrimary)
            separator.setBackgroundColor(primary)
        }
    }

    var pages = pages
        set(value) {
            field = value
            this.notifyDataSetChanged()
        }

    interface ThumbnailGetter {
        suspend fun getThumbnailForBackground(backgroundId: String, width: Int, height: Int) : Bitmap?
    }

    fun interface OnLateLoadedListener {
        fun onLateLoaded(pageItemViewHolder: PageItemViewHolder)
    }

    override fun getItemViewType(position: Int): Int {
        return if(position == 0) VIEWTYPE_WELCOME else VIEWTYPE_PAGEITEM
    }

    //View holder creation and setup
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEWTYPE_WELCOME -> {
                WelcomeViewHolder(inflater.inflate(R.layout.browser_home_item, parent, false))
            }
            VIEWTYPE_PAGEITEM -> {
                PageItemViewHolder(inflater.inflate(R.layout.browser_page_item, parent, false))
            }
            else -> throw IllegalArgumentException("invalid View Type!")
        }
    }

    //View holder assignment
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType) {
            VIEWTYPE_WELCOME -> {}
            VIEWTYPE_PAGEITEM -> {
                bindPageItemHolder(holder as PageItemViewHolder, position)
            }
        }
    }

    private fun bindPageItemHolder(pageItem: PageItemViewHolder, position: Int) = scope.launch {

        val page = pages[position-1] //Welcome item take position one, shifting all the pages.

        pageItem.title = page.title
        pageItem.detail = page.getCreatedTimestamp()
        pageItem.thumbnail = null //Clear any old dirty data before we retrieve new thumbnail

        pageItem.setTransitionId(page.uid.toString())

        val white = context.getColor(android.R.color.white)
        val black = context.getColor(android.R.color.black)
        pageItem.setColoring(white, black)


        val backgroundId = page.backgroundId
        if(backgroundId != null) {
            pageItem.isLoading = true

            val thumbWidth = pageItem.thumbnailImageView.width
            val thumbHeight = pageItem.thumbnailImageView.height

            withContext(Dispatchers.IO) {
                val bitmap = thumbnailGetter.getThumbnailForBackground(
                    backgroundId,
                    thumbWidth,
                    thumbHeight
                )

                if (bitmap != null) {
                    Palette.from(bitmap).generate {
                        it?: return@generate
                        pageItem.setColoring(it.getLightVibrantColor(white), black)
                    }
                }

                withContext(Dispatchers.Main) {
                    pageItem.thumbnail = bitmap
                    pageItem.isLoading = false
                    onLateLoadedListener?.onLateLoaded(pageItem)
                }

            }

            /*
            val mainViewModel: MainViewModel by fragment.activityViewModels()
            mainViewModel.lastOpenedPageId?.let { id ->
                if (page.uid == id) fragment.startPostponedEnterTransition()
            }

             */
        }
    }

    override fun getItemCount(): Int = 1 + pages.size //1 for welcome screen
}