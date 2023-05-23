package com.naf.npad.android.browser

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.naf.npad.android.MainActivity
import com.naf.npad.R
import com.naf.npad.databinding.BrowserMainBinding
import com.naf.npad.android.util.NPMLImporter
import com.naf.npad.android.MainViewModel
import com.naf.npad.android.data.PageInfo
import kotlinx.coroutines.*

class BrowserFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    interface BrowserFragmentDelegate {
        //Passing view holder is required in order to setup shared view transition animations
        //We could not pass it and require a list lookup in the delegate, but that would be wasteful.
        fun onPageSelected(pageId: Int, viewHolder: PageItemViewHolder)
    }

    var delegate : BrowserFragmentDelegate? = null

    private lateinit var views : BrowserMainBinding
    private val mainViewModel : MainViewModel by activityViewModels()

    private val thumbnailLoader = object : PagesAdapter.ThumbnailLoader() {

        override fun getThumbnailForBackground(backgroundId: String, width: Int, height: Int): Bitmap? {
            return runBlocking {
                //Todo, plugin actual view pixel size
                val w = requireContext().resources.displayMetrics.widthPixels / 3
                val h = requireContext().resources.displayMetrics.heightPixels / 3
                return@runBlocking withContext(Dispatchers.IO) {
                    return@withContext mainViewModel.getThumbnailForBackground(backgroundId, w, h)
                }
            }
        }
    }

    private var adapter = PagesAdapter(this,listOf(), thumbnailLoader)

    private val pageListClickListener = object : RecyclerTouchToClickListener.ClickListener() {

        override fun onClick(view: View, adapterPosition: Int) {
            if(adapterPosition == 0) { newPage(); return }

            val selectedPage = adapter.pages[-1 + adapterPosition] //-1 counts as the welcome tile
            val viewHolder = views.docmanDocumentList.findViewHolderForAdapterPosition(adapterPosition)?: return
            delegate?.onPageSelected(selectedPage.uid, viewHolder as PageItemViewHolder)
        }

        override fun onLongClick(view: View, adapterPosition: Int) {
            if(adapterPosition == 0) return

            val page = adapter.pages[-1 + adapterPosition]
            presentPageActionMenu(view, page)
        }
    }

    fun presentPageActionMenu(pageView: View, page: PageInfo) =
        PopupMenu(requireContext(), pageView, Gravity.CENTER_HORIZONTAL).apply {
            inflate(R.menu.browser_page_actions)

            setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.menu_pageitem_delete -> mainViewModel.deletePage(page)
                    R.id.menu_pageitem_duplicate -> mainViewModel.duplicatePage(page)
                    R.id.menu_pageitem_rename -> renamePage(page)
                }
                false
            }
            show()
        }

    private fun renamePage(page: PageInfo) = NameDocumentDialog().apply {
        onDialogSuccess = { title ->
            page.title = title
            mainViewModel.updatePage(page)
        }
        show(this@BrowserFragment.requireActivity().supportFragmentManager, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        views = BrowserMainBinding.inflate(inflater, container, false)

        mainViewModel.pagesDetail.observe(viewLifecycleOwner) { pages ->
            adapter.pages = pages.sortedBy { it.modified }.reversed()
        }

        views.docmanDocumentList.adapter = adapter
        views.docmanDocumentList.addOnItemTouchListener(
            RecyclerTouchToClickListener(requireContext(), views.docmanDocumentList, pageListClickListener)
        )

        setupTransitions()

        return views.root
    }

    private fun setupTransitions(){
        postponeEnterTransition()
    }

    private fun getLastPosition() : Int? = runBlocking {
        val lastOpenPageId = mainViewModel.lastOpenedPageId?: return@runBlocking null
        val lastOpenedPageDetail = mainViewModel.getPageInfoWithId(lastOpenPageId)?: return@runBlocking null
        val position = adapter.pages.indexOf(lastOpenedPageDetail)
        return@runBlocking if(position > -1) position else null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scrollToLastPageItem()
    }

    private fun scrollToLastPageItem() = runBlocking {
        launch {
            val layoutManager = views.docmanDocumentList.layoutManager?: return@launch
            val lastPageId = mainViewModel.lastOpenedPageId?: return@launch
            val lastPageDetails = mainViewModel.getPageInfoWithId(lastPageId)?: return@launch
            val position = adapter.pages.indexOf(lastPageDetails)
            val laidOutView = views.docmanDocumentList.layoutManager?.findViewByPosition(position)
            if(laidOutView == null ||
                layoutManager.isViewPartiallyVisible(laidOutView, false, true)) {
                views.docmanDocumentList.post { layoutManager.scrollToPosition(position) }
            }
        }
    }

    private fun newPage() = NameDocumentDialog().apply {
        onDialogSuccess = { documentName ->
            mainViewModel.newPage(documentName)
        }
        show(this@BrowserFragment.requireActivity().supportFragmentManager, null)
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            R.id.drawer_action_import -> NPMLImporter(requireActivity()).importDocument()
            R.id.editor_action_gotoSetting -> (requireActivity() as? MainActivity)?.openSettings()
            R.id.documentmanager_action_new_document -> newPage()
        }
        return true
    }

    interface ViewHolderListener {
        fun onLoadComplete(position: Int)
        fun onItemClicked()
        fun onItemLongClicked()
    }

    class WelcomeViewHolder(itemView: View, private var fragment: Fragment) : RecyclerView.ViewHolder(itemView) {
        private val toolbar : Toolbar = itemView.findViewById(R.id.home_toolbar)
        init {
            toolbar.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.editor_action_gotoSetting -> {
                        (fragment.requireActivity() as? MainActivity)?.openSettings()
                        return@setOnMenuItemClickListener true
                    }
                    else -> return@setOnMenuItemClickListener false
                }
            }
        }

    }

    class PageItemViewHolder(itemView: View, viewHolderListener: ViewHolderListener? = null) : RecyclerView.ViewHolder(itemView) {
        var titleTextView : TextView = itemView.findViewById(R.id.pageItem_title_textView)
        var timeStampTextView : TextView = itemView.findViewById(R.id.pageItem_created_textView)
        var thumbnailImageView : ImageView = itemView.findViewById(R.id.pageItem_thumbnail_imageView)

        fun setTitle(title: String?) {
            titleTextView.text = if(title.isNullOrEmpty()) "[Untitled]" else title
        }

        fun setTimeStamp(timestamp: String?) {
            timeStampTextView.text = timestamp
        }

        fun setThumbnail(bitmap: Bitmap?) {
            thumbnailImageView.setImageBitmap(bitmap)
        }

        fun setTransitionId(id: String){
            ViewCompat.setTransitionName(thumbnailImageView, "$id-background")
            ViewCompat.setTransitionName(titleTextView, "$id-title")
        }
    }

    class PagesAdapter(
        private val fragment: Fragment,
        pages: List<com.naf.npad.android.data.PageInfo>,
        private val thumbnailLoader: ThumbnailLoader,
        ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val VIEWTYPE_WELCOME = 0
            const val VIEWTYPE_PAGEITEM = 1
        }

        var pages = pages
        set(value) {
            field = value
            this.notifyDataSetChanged()
        }

        abstract class ThumbnailLoader {
            abstract fun getThumbnailForBackground(backgroundId: String, width: Int, height: Int) : Bitmap?
        }

        override fun getItemViewType(position: Int): Int {
            return if(position == 0) VIEWTYPE_WELCOME else VIEWTYPE_PAGEITEM
        }

        //View holder creation and setup
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layout = when(viewType) {
                VIEWTYPE_WELCOME -> R.layout.browser_home_item
                else -> R.layout.browser_page_item
            }
            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)

            val holder = when(viewType) {
                VIEWTYPE_WELCOME -> WelcomeViewHolder(view, fragment)
                else -> PageItemViewHolder(view)
            }
            return holder
        }

        //View holder assignment
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) : Unit {
            when(holder.itemViewType) {
                VIEWTYPE_PAGEITEM -> {
                    bindPageItemHolder(holder as PageItemViewHolder, position)
                }
            }
        }

        private fun bindPageItemHolder(holder: PageItemViewHolder, position: Int) = runBlocking {

            val page = pages[position-1] //Welcome item take position one, shifting all the pages.

            holder.setTitle(page.title)
            holder.setTimeStamp(page.getCreatedTimestamp())
            holder.setThumbnail(null) //Clear any old dirty data before we retrieve new thumbnail

            holder.setTransitionId(page.uid.toString())

            page.backgroundId?.let { backgroundId ->
                val thumbWidth = holder.thumbnailImageView.width
                val thumbHeight = holder.thumbnailImageView.height
                val bitmap =
                    thumbnailLoader.getThumbnailForBackground(
                        backgroundId,
                        thumbWidth,
                        thumbHeight
                    )
                Glide.with(this@PagesAdapter.fragment).load(bitmap)
                    .into(holder.thumbnailImageView)

                launch {
                    val mainViewModel: MainViewModel by fragment.activityViewModels()
                    mainViewModel.lastOpenedPageId?.let { id ->
                        if (page.uid == id) fragment.startPostponedEnterTransition()
                    }
                }
            }
        }

        override fun getItemCount(): Int = 1 + pages.size //1 for welcome screen
    }

    class RecyclerTouchToClickListener(
        context: Context,
        recyclerView: RecyclerView,
        val clickListener: ClickListener
        ) : RecyclerView.OnItemTouchListener {

        abstract class ClickListener {
            abstract fun onClick(view: View, adapterPosition: Int)
            abstract fun onLongClick(view: View, adapterPosition: Int)
        }

        //Process Events
        private val gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

                //We have to override and return true per Docs. !Don't remove!
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    val child = recyclerView.findChildViewUnder(e.x, e.y) ?: return false
                    clickListener.onClick(child, recyclerView.getChildAdapterPosition(child))
                    return true
                }

            override fun onLongPress(e: MotionEvent) {
                val child = recyclerView.findChildViewUnder(e.x, e.y) ?: return
                clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child))
            }
        })

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            // We hand gesture detector the event info.
            // Returns true if handled successfully.
            // Handy.
            return gestureDetector.onTouchEvent(e)
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean){}
    }
}