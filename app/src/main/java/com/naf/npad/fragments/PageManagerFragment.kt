package com.naf.npad.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naf.npad.MainActivity
import com.naf.npad.R
import com.naf.npad.Utls
import com.naf.npad.databinding.FragmentPageManagerBinding
import com.naf.npad.dialogs.NameDocumentDialog
import com.naf.npad.repository.PageEntity
import com.naf.npad.util.NPMLImporter
import com.naf.npad.viewmodels.AppViewModel
import com.naf.npad.views.BounceEdgeEffect
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.CompletableFuture.runAsync

class PageManagerFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    interface PageManagerFragmentDelegate {
        fun onPageSelected(page: PageEntity)
    }

    var delegate : PageManagerFragmentDelegate? = null

    private lateinit var binding : FragmentPageManagerBinding
    private val appViewModel : AppViewModel by activityViewModels()

    private val thumbnailProvider = object : PagesAdapter.ThumbnailProvider() {
        override fun getThumbnailForBackground(backgroundId: String, width: Int, height: Int): Bitmap? = runBlocking {
            val w = requireContext().resources.displayMetrics.widthPixels / 2
            val h = requireContext().resources.displayMetrics.heightPixels / 2
            return@runBlocking appViewModel.getThumbnailForBackground(backgroundId, w, h)
        }
    }

    private var adapter = PagesAdapter(listOf(), thumbnailProvider)

    private val pageListClickListener = object : RecyclerTouchListener.ClickListener() {

        override fun onClick(view: View, adapterPosition: Int) {
            val selectedPage = adapter.pages[adapterPosition]
            delegate?.onPageSelected(selectedPage)
            appViewModel.currentPage.postValue(selectedPage)
            appViewModel.drawerOpen.postValue(false)
        }

        override fun onLongClick(view: View, adapterPosition: Int) {
            val page = adapter.pages[adapterPosition]
            val menu = PopupMenu(requireContext(), view, Gravity.CENTER)
            menu.inflate(R.menu.pageitem_context_actions)
            menu.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.action_pageitem_delete -> {
                        appViewModel.deletePage(page)
                    }
                    R.id.action_pageitem_duplicate -> {
                        appViewModel.duplicatePage(page)
                    }
                    R.id.action_pageitem_rename -> {
                        val dialog = NameDocumentDialog()
                        dialog.onDialogFinished = { title ->
                            page.title = title
                            appViewModel.updatePage(page)
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

        binding = FragmentPageManagerBinding.inflate(inflater, container, false)

        binding.pageManagerToolbar.setOnMenuItemClickListener(this)

        appViewModel.pages.observe(viewLifecycleOwner) { pages ->
            adapter.pages = pages.sortedBy { it.modified }.reversed()
        }

        binding.pageManagerFabNew.setOnClickListener {
            val dialog = NameDocumentDialog()
            dialog.onDialogFinished = { documentName ->
                appViewModel.newPage(documentName)
            }
            activity?.let { dialog.show(it.supportFragmentManager, null) }
        }

        binding.pageManagerPageList.adapter = adapter
        binding.pageManagerPageList.layoutManager = GridLayoutManager(requireContext(), 2)

        val recyclerTouchListener = RecyclerTouchListener(requireContext(), binding.pageManagerPageList, pageListClickListener)

        binding.pageManagerPageList.addOnItemTouchListener(recyclerTouchListener)
        binding.pageManagerPageList.edgeEffectFactory = BounceEdgeEffect(binding.pageManagerPageList)

        return binding.root
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            R.id.drawer_action_import -> NPMLImporter(requireActivity()).importDocument()
            R.id.editor_action_gotoSetting -> startSettings()
        }
        return true
    }

    private fun startSettings() {
        val settingsFragment = SettingsFragment()

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, settingsFragment)
            .addToBackStack(null)
            .commit()

        appViewModel.drawerOpen.value = false
    }

    class PagesAdapter(
        pages: List<PageEntity>,
        private val thumbnailProvider: ThumbnailProvider
        ) : RecyclerView.Adapter<PagesAdapter.ViewHolder>() {

        var pages = pages
        set(value) {
            field = value
            this.notifyDataSetChanged()
        }

        abstract class ThumbnailProvider {
            abstract fun getThumbnailForBackground(backgroundId: String, width: Int, height: Int) : Bitmap?
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private var titleTextView : TextView = itemView.findViewById(R.id.pageItem_title_textView)
            private var timeStampTextView : TextView = itemView.findViewById(R.id.pageItem_created_textView)
            private var thumbnailImageView : ImageView = itemView.findViewById(R.id.pageItem_thumbnail_imageView)

            fun setTitle(title: String?) {
                titleTextView.text = if(title.isNullOrEmpty()) "[Untitled]" else title
            }

            fun setTimeStamp(timestamp: String?) {
                timeStampTextView.text = timestamp
            }

            fun setThumbnail(bitmap: Bitmap?) {
                thumbnailImageView.setImageBitmap(bitmap)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.page_list_item,
                    parent,
                    false)



            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val page = pages[position]

            holder.setTitle(page.title)
            holder.setTimeStamp(page.getCreatedTimestamp())
            holder.setThumbnail(null) //Clear any old dirty data before we retrieve new thumbnail

            page.backgroundId?.let { backgroundId ->
                val bitmap = thumbnailProvider.getThumbnailForBackground(backgroundId, 0, 0)
                bitmap?.let { holder.setThumbnail(it) }
            }
        }

        override fun getItemCount(): Int = pages.size
    }

    class RecyclerTouchListener(
        context: Context,
        recyclerView: RecyclerView,
        val clickListener: ClickListener
    ) : RecyclerView.OnItemTouchListener {

        private val gestureDetector: GestureDetector = GestureDetector(context, object :
            GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val child = recyclerView.findChildViewUnder(e.x, e.y) ?: return
                clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child))
            }
        })

        abstract class ClickListener {
            abstract fun onClick(view: View, adapterPosition: Int)
            abstract fun onLongClick(view: View, adapterPosition: Int)
        }

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            val child = rv.findChildViewUnder(e.x, e.y) ?: return false
            if(gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildAdapterPosition(child))
                return true
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean){}
    }



}