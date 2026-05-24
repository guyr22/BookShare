package com.example.bookshare.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookshare.R
import com.example.bookshare.databinding.FragmentAddEditBookBinding
import com.example.bookshare.local.AppDatabase
import com.example.bookshare.local.Book
import com.example.bookshare.repository.AppResult
import com.example.bookshare.repository.BookRepository
import com.example.bookshare.network.NO_INTERNET_MESSAGE
import com.example.bookshare.network.isNetworkAvailable
import com.example.bookshare.ui.search.BookSearchResultAdapter
import com.example.bookshare.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class AddEditBookFragment : Fragment() {

    private var binding: FragmentAddEditBookBinding? = null
    private var viewModel: MainViewModel? = null

    private val args: AddEditBookFragmentArgs by navArgs()
    private val isEditMode: Boolean get() = args.bookId.isNotBlank()

    /** Latest fully-loaded book in edit mode (carries id, ownerId, lastUpdated). */
    private var loadedBook: Book? = null

    /** Cover URL from Google Books search or the loaded book — used when no bitmap is staged. */
    private var pendingCoverUrl: String = ""

    /** Cover bitmap picked by the user — takes priority over pendingCoverUrl on save. */
    private var pendingCoverBitmap: Bitmap? = null

    /** Whether we've already navigated back so we ignore further LiveData emissions. */
    private var hasNavigatedBack: Boolean = false

    private var searchAdapter: BookSearchResultAdapter? = null
    private val searchHandler = Handler(Looper.getMainLooper())
    private var pendingQuery: String = ""
    private val searchRunnable = Runnable { triggerSearch(pendingQuery) }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) stageCoverBitmap(bitmap)
        else Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = decodeBitmap(uri)
            if (bitmap != null) stageCoverBitmap(bitmap)
            else Toast.makeText(context, "Couldn't read that image", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAddEditBookBinding.inflate(layoutInflater, container, false)
        applyTopInset()
        setupViewModel()
        setupToolbar()
        setupSearch()
        setupCoverImagePicker()
        setupSaveAndDelete()
        observeViewModel()
        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    private fun setupViewModel() {
        val ctx = requireContext().applicationContext
        val bookRepository = BookRepository(AppDatabase.getInstance(ctx).bookDao())
        viewModel = ViewModelProvider(this, MainViewModel.Factory(bookRepository))[MainViewModel::class.java]
    }

    private fun applyTopInset() {
        val toolbar = binding?.addEditToolbar ?: return
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            view.updatePadding(top = top)
            insets
        }
    }

    private fun setupToolbar() {
        binding?.titleTextView?.setText(
            if (isEditMode) R.string.addedit_edit_title else R.string.addedit_add_title
        )
        binding?.backButton?.setOnClickListener {
            it.findNavController().popBackStack()
        }
        binding?.deleteButton?.visibility = if (isEditMode) View.VISIBLE else View.GONE
    }

    private fun setupSearch() {
        searchAdapter = BookSearchResultAdapter { book ->
            applyGoogleBooksMatch(book)
            hideResults()
            hideKeyboard()
        }
        binding?.searchResultsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        binding?.searchResultsRecyclerView?.adapter = searchAdapter

        // Live search: fire once typing pauses, and only from MIN_QUERY_LENGTH letters.
        // The debounce also keeps request volume low (Google Books rate-limits hard).
        binding?.searchEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                searchHandler.removeCallbacks(searchRunnable)
                if (query.length >= MIN_QUERY_LENGTH) {
                    pendingQuery = query
                    searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS)
                } else {
                    binding?.searchProgress?.visibility = View.GONE
                    hideResults()
                }
            }
        })

        // Pressing the keyboard's search action runs the query immediately.
        binding?.searchEditText?.setOnEditorActionListener { editText, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = editText.text.toString().trim()
                searchHandler.removeCallbacks(searchRunnable)
                if (query.length >= MIN_QUERY_LENGTH) triggerSearch(query)
                true
            } else {
                false
            }
        }
    }

    private fun triggerSearch(query: String) {
        binding?.searchProgress?.visibility = View.VISIBLE
        viewModel?.searchGoogleBooks(query)
    }

    private fun hideResults() {
        searchAdapter?.submit(emptyList())
        binding?.searchResultsRecyclerView?.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun setupCoverImagePicker() {
        binding?.coverImageView?.setOnClickListener { showCoverImageSourceChooser() }
    }

    private fun showCoverImageSourceChooser() {
        val options = arrayOf("Take a photo", "Choose from gallery")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.addedit_cover_source_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
            .show()
    }

    private fun stageCoverBitmap(bitmap: Bitmap) {
        pendingCoverBitmap = bitmap
        pendingCoverUrl = ""
        val target = binding?.coverImageView ?: return
        target.scaleType = ImageView.ScaleType.CENTER_CROP
        target.setImageBitmap(bitmap)
    }

    private fun setupSaveAndDelete() {
        binding?.saveButton?.setOnClickListener { onSaveClicked() }
        binding?.deleteButton?.setOnClickListener { onDeleteClicked() }
    }

    private fun observeViewModel() {
        val vm = viewModel ?: return

        // Edit mode: load the existing book once and prefill the form.
        if (isEditMode) {
            val source = vm.getBookById(args.bookId)
            source.observe(viewLifecycleOwner, object : Observer<Book?> {
                override fun onChanged(value: Book?) {
                    if (value == null) return
                    loadedBook = value
                    pendingCoverUrl = value.coverUrl
                    populateFields(value)
                    // Stop observing after the first non-null hit so user edits aren't clobbered.
                    source.removeObserver(this)
                }
            })
        }

        // Save / delete result.
        vm.bookOperation.observe(viewLifecycleOwner) { result ->
            if (hasNavigatedBack || result == null) return@observe
            binding?.saveProgressOverlay?.visibility = View.GONE
            when (result) {
                is AppResult.Success<*> -> {
                    hasNavigatedBack = true
                    view?.findNavController()?.popBackStack()
                }
                is AppResult.Error -> {
                    Toast.makeText(
                        context,
                        getString(R.string.addedit_save_failed, result.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Google Books search result → show the matches list to pick from.
        vm.searchResult.observe(viewLifecycleOwner) { result ->
            binding?.searchProgress?.visibility = View.GONE
            val currentLen = binding?.searchEditText?.text?.toString()?.trim()?.length ?: 0
            when (result) {
                is AppResult.Success -> {
                    if (result.data.isEmpty() || currentLen < MIN_QUERY_LENGTH) {
                        hideResults()
                    } else {
                        searchAdapter?.submit(result.data)
                        binding?.searchResultsRecyclerView?.visibility = View.VISIBLE
                    }
                }
                is AppResult.Error -> {
                    hideResults()
                    Toast.makeText(
                        context,
                        getString(R.string.addedit_search_failed, result.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
                null -> Unit
            }
        }
    }

    private fun populateFields(book: Book) {
        binding?.titleEditText?.setText(book.title)
        binding?.authorEditText?.setText(book.author)
        binding?.descriptionEditText?.setText(book.description)
        binding?.ratingBar?.rating = book.rating.toFloat()
        binding?.reviewEditText?.setText(book.review)
        loadCoverIntoView(book.coverUrl)
    }

    private fun applyGoogleBooksMatch(match: Book) {
        binding?.titleEditText?.setText(match.title)
        binding?.authorEditText?.setText(match.author)
        binding?.descriptionEditText?.setText(match.description)
        // Only update the cover from Google Books if the user hasn't picked their own photo.
        if (pendingCoverBitmap == null) {
            pendingCoverUrl = match.coverUrl
            loadCoverIntoView(match.coverUrl)
        }
    }

    private fun loadCoverIntoView(url: String) {
        val target = binding?.coverImageView ?: return
        if (url.isBlank()) {
            target.setImageDrawable(null)
        } else {
            Picasso.get().load(url).fit().centerCrop().into(target)
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return try {
            val resolver = requireContext().contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(resolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(resolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun onSaveClicked() {
        if (!requireContext().isNetworkAvailable()) {
            Toast.makeText(context, NO_INTERNET_MESSAGE, Toast.LENGTH_LONG).show()
            return
        }

        val title = binding?.titleEditText?.text?.toString()?.trim().orEmpty()
        val author = binding?.authorEditText?.text?.toString()?.trim().orEmpty()
        val description = binding?.descriptionEditText?.text?.toString()?.trim().orEmpty()

        if (title.isEmpty() || author.isEmpty()) {
            Toast.makeText(context, R.string.addedit_required_field, Toast.LENGTH_SHORT).show()
            return
        }

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val existing = loadedBook
        val rating = binding?.ratingBar?.rating?.toInt() ?: 0
        val review = binding?.reviewEditText?.text?.toString()?.trim().orEmpty()

        val book = if (existing != null) {
            existing.copy(
                title = title,
                author = author,
                description = description,
                coverUrl = if (pendingCoverBitmap != null) "" else pendingCoverUrl,
                rating = rating,
                review = review
            )
        } else {
            Book(
                id = "",
                title = title,
                author = author,
                description = description,
                coverUrl = if (pendingCoverBitmap != null) "" else pendingCoverUrl,
                ownerId = currentUid,
                rating = rating,
                review = review
            )
        }

        binding?.saveProgressOverlay?.visibility = View.VISIBLE
        viewModel?.saveBook(book, coverBitmap = pendingCoverBitmap)
    }

    private fun onDeleteClicked() {
        if (!requireContext().isNetworkAvailable()) {
            Toast.makeText(context, NO_INTERNET_MESSAGE, Toast.LENGTH_LONG).show()
            return
        }
        val book = loadedBook ?: return
        binding?.saveProgressOverlay?.visibility = View.VISIBLE
        viewModel?.deleteBook(book)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchHandler.removeCallbacks(searchRunnable)
        searchAdapter = null
        binding = null
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 4
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
