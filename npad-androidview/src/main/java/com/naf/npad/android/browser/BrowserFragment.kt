package com.naf.npad.android.browser

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.naf.npad.android.MainActivity
import com.naf.npad.R
import com.naf.npad.databinding.BrowserMainBinding
import com.naf.npad.android.util.NPMLImporter
import com.naf.npad.android.MainViewModel
import com.naf.npad.android.data.PageInfo
import kotlinx.coroutines.*
import kotlin.math.roundToInt

class BrowserFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    interface BrowserFragmentDelegate {
        //Passing view holder is required in order to setup shared view transition animations
        //We could not pass it and require a list lookup in the delegate, but that would be wasteful.
        fun onPageSelected(pageId: Int, viewHolder: PagesAdapter.PageItemViewHolder)
    }

    var delegate : BrowserFragmentDelegate? = null

    private lateinit var views : BrowserMainBinding
    private val mainViewModel : MainViewModel by activityViewModels()

    private val thumbnailGetter = object: PagesAdapter.ThumbnailGetter {
        override suspend fun getThumbnailForBackground(
            backgroundId: String,
            width: Int,
            height: Int
        ): Bitmap? {
                //Todo, plugin actual view pixel size
                val w = requireContext().resources.displayMetrics.widthPixels / 3
                val h = requireContext().resources.displayMetrics.heightPixels / 3
                return withContext(Dispatchers.IO) {
                    return@withContext mainViewModel.getThumbnailForBackground(backgroundId, w, h)
                }
        }

    }

    private val handleLateLoad = PagesAdapter.OnLateLoadedListener { pageItem->
        val display = resources.displayMetrics
        val isOnScreen =
            pageItem.position.intersect(0, 0, display.widthPixels, display.heightPixels)
        if (isOnScreen) {
            //Animate the thumbnail
//            ValueAnimator.ofFloat(0.0f, 1.0f).apply {
//                duration = 700
//                addUpdateListener { va ->
//                    pageItem.thumbnailImageView.alpha = va.animatedValue as Float
//                }
//                start()
//            }
        }
    }

    private lateinit var adapter : PagesAdapter

    private val pageListClickListener = object : RecyclerTouchToClickListener.ClickListener() {

        override fun onClick(view: View, adapterPosition: Int) {
            if(adapterPosition == 0) { newPage(); return }

            val selectedPage = adapter.pages[-1 + adapterPosition] //-1 counts as the welcome tile
            val viewHolder = views.browserPageList.findViewHolderForAdapterPosition(adapterPosition)?: return
            delegate?.onPageSelected(selectedPage.uid, viewHolder as PagesAdapter.PageItemViewHolder)
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

        adapter =  PagesAdapter(requireContext(), listOf(), thumbnailGetter, handleLateLoad)

        mainViewModel.pagesDetail.observe(viewLifecycleOwner) { pages ->
            adapter.pages = pages.sortedBy { it.modified }.reversed()
        }

        views.browserPageList.adapter = adapter
        views.browserPageList.addOnItemTouchListener(
            RecyclerTouchToClickListener(requireContext(), views.browserPageList, pageListClickListener)
        )

        setupTransitions()

        return views.root
    }

    private fun setupTransitions(){
        //postponeEnterTransition()
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

        /*//Setup recycler padding
        val itemHeightDP = 220
        val itemHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, itemHeightDP.toFloat(), requireActivity().resources.displayMetrics)
        val itemHalfWidth = (itemHeight * 0.5625 / 2)
        val listHalfWidth = (requireActivity().resources.displayMetrics.widthPixels / 2)
        val padding = (listHalfWidth - itemHalfWidth).roundToInt()

        views.browserPageList.setPadding(padding, 0,padding,0)*/
    }

    private fun scrollToLastPageItem() = runBlocking {
        launch {
            val layoutManager = views.browserPageList.layoutManager?: return@launch
            val lastOpenedPageId = mainViewModel.lastOpenedPageId?: return@launch
            val lastOpenedPage = mainViewModel.getPageInfoWithId(lastOpenedPageId)?: return@launch
            val position = adapter.pages.indexOf(lastOpenedPage)
            val laidOutView = views.browserPageList.layoutManager?.findViewByPosition(position)
            if(laidOutView == null ||
                layoutManager.isViewPartiallyVisible(laidOutView, false, true)) {
                views.browserPageList.post { layoutManager.scrollToPosition(position) }
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