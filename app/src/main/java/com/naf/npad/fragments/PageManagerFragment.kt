package com.naf.npad.fragments

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
import com.naf.npad.R
import com.naf.npad.databinding.LayoutPagemanagerBinding
import com.naf.npad.dialogs.NameDocumentDialog
import com.naf.npad.repository.PageDetail
import com.naf.npad.util.NPMLImporter
import com.naf.npad.viewmodels.MainViewModel
import kotlinx.coroutines.*

class PageManagerFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    interface PageManagerFragmentDelegate {
        //Passing view holder is required in order to setup shared view transition animations
        //We could not pass it and require a list lookup in the delegate, but that would be wasteful.
        fun onPageSelected(pageId: Int, viewHolder: PageItemViewHolder)
    }

    var delegate : PageManagerFragmentDelegate? = null

    private lateinit var views : LayoutPagemanagerBinding
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
            val selectedPage = adapter.pages[adapterPosition]
            val viewHolder = views.docmanDocumentList.findViewHolderForAdapterPosition(adapterPosition)?: return
            delegate?.onPageSelected(selectedPage.uid, viewHolder as PageItemViewHolder)
        }

        override fun onLongClick(view: View, adapterPosition: Int) {
            val page = adapter.pages[adapterPosition]
            val menu = PopupMenu(requireContext(), view, Gravity.CENTER)
            menu.inflate(R.menu.menu_document_actions)
            menu.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.action_pageitem_delete -> {
                        mainViewModel.deletePage(page)
                    }
                    R.id.action_pageitem_duplicate -> {
                        mainViewModel.duplicatePage(page)
                    }
                    R.id.action_pageitem_rename -> {
                        val dialog = NameDocumentDialog()
                        dialog.onDialogFinished = { title ->
                            page.title = title
                            mainViewModel.updatePage(page)
                        }
                        dialog.show(requireActivity().supportFragmentManager, null)
                    }
                }
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        views = LayoutPagemanagerBinding.inflate(inflater, container, false)

        views.docmanToolbar.setOnMenuItemClickListener(this)

        mainViewModel.pagesDetail.observe(viewLifecycleOwner) { pages ->
            adapter.pages = pages.sortedBy { it.modified }.reversed()
        }

        views.docmanDocumentList.adapter = adapter
        views.docmanDocumentList.addOnItemTouchListener(
            RecyclerTouchToClickListener(requireContext(), views.docmanDocumentList, pageListClickListener))

        setupTransitions()

        return views.root
    }

    private fun setupTransitions(){

        postponeEnterTransition()

    }

    private fun getLastPosition() : Int? = runBlocking {
        val lastOpenPageId = mainViewModel.lastOpenedPageId?: return@runBlocking null
        val lastOpenedPageDetail = mainViewModel.getPageDetailsWithId(lastOpenPageId)?: return@runBlocking null
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
            val lastPageDetails = mainViewModel.getPageDetailsWithId(lastPageId)?: return@launch
            val position = adapter.pages.indexOf(lastPageDetails)
            val laidOutView = views.docmanDocumentList.layoutManager?.findViewByPosition(position)
            if(laidOutView == null ||
                layoutManager.isViewPartiallyVisible(laidOutView, false, true)) {
                views.docmanDocumentList.post { layoutManager.scrollToPosition(position) }
            }
        }
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            R.id.drawer_action_import -> NPMLImporter(requireActivity()).importDocument()
            R.id.editor_action_gotoSetting -> startSettings()
            R.id.documentmanager_action_new_document -> {
                val dialog = NameDocumentDialog()
                dialog.onDialogFinished = { documentName ->
                    mainViewModel.newPage(documentName)
                }
                activity?.let { dialog.show(it.supportFragmentManager, null) }
            }
        }
        return true
    }

    private fun startSettings() {
        val settingsFragment = SettingsFragment()

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, settingsFragment)
            .addToBackStack(null)
            .commit()
    }

    interface ViewHolderListener {
        fun onLoadComplete(position: Int)
        fun onItemClicked()
        fun onItemLongClicked()
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
        pages: List<PageDetail>,
        private val thumbnailLoader: ThumbnailLoader,
        ) : RecyclerView.Adapter<PageItemViewHolder>() {

        var pages = pages
        set(value) {
            field = value
            this.notifyDataSetChanged()
        }

        abstract class ThumbnailLoader {
            abstract fun getThumbnailForBackground(backgroundId: String, width: Int, height: Int) : Bitmap?
        }

        //View holder creation and setup
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.layout_document_item,
                    parent,
                    false)
            return PageItemViewHolder(view)
        }

        //View holder assignment
        override fun onBindViewHolder(holder: PageItemViewHolder, position: Int) : Unit = runBlocking {
            val page = pages[position]

            holder.setTitle(page.title)
            holder.setTimeStamp(page.getCreatedTimestamp())
            holder.setThumbnail(null) //Clear any old dirty data before we retrieve new thumbnail

            holder.setTransitionId(page.uid.toString())

            page.backgroundId?.let { backgroundId ->
                val thumbWidth = holder.thumbnailImageView.width
                val thumbHeight = holder.thumbnailImageView.height
                val bitmap =
                    thumbnailLoader.getThumbnailForBackground(backgroundId, thumbWidth, thumbHeight)
                Glide.with(this@PagesAdapter.fragment).load(bitmap).into(holder.thumbnailImageView)

                launch {
                    val mainViewModel: MainViewModel by fragment.activityViewModels()
                    mainViewModel.lastOpenedPageId?.let { id ->
                        if (page.uid == id) fragment.startPostponedEnterTransition()
                    }
                }
            }
        }

        override fun getItemCount(): Int = pages.size
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