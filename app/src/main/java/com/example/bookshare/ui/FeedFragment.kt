package com.example.bookshare.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookshare.R
import com.example.bookshare.databinding.FragmentFeedBinding
import com.example.bookshare.local.AppDatabase
import com.example.bookshare.repository.AppResult
import com.example.bookshare.repository.AuthRepository
import com.example.bookshare.repository.BookRepository
import com.example.bookshare.repository.UserRepository
import com.example.bookshare.ui.feed.BookAdapter
import com.example.bookshare.viewmodel.AuthViewModel
import com.example.bookshare.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class FeedFragment : Fragment() {

    private var binding: FragmentFeedBinding? = null
    private var adapter: BookAdapter? = null
    private var viewModel: MainViewModel? = null
    private var authViewModel: AuthViewModel? = null

    /** ownerId → display name, populated from the synced user cache. */
    private val usersById = mutableMapOf<String, String>()

    /** ownerId → avatar URL, populated from the synced user cache. */
    private val avatarsById = mutableMapOf<String, String>()

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy <= 0) return
            val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
            val lastVisible = lm.findLastVisibleItemPosition()
            val total = lm.itemCount
            if (total > 0 && lastVisible >= total - LOAD_MORE_THRESHOLD) {
                viewModel?.loadMoreBooks()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFeedBinding.inflate(layoutInflater, container, false)
        applyTopInset()
        setupViewModels()
        setupRecyclerView()
        setupToolbar()
        setupFab()
        setupSearch()
        observeBooks()
        observeUsers()
        observeCurrentUser()
        return binding?.root
    }

    override fun onStart() {
        super.onStart()
        viewModel?.startRealtimeSync(viewLifecycleOwner.lifecycleScope)
        authViewModel?.syncUsers()
    }

    override fun onStop() {
        super.onStop()
        viewModel?.stopRealtimeSync()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        binding?.syncProgressBar?.visibility = View.VISIBLE
        viewModel?.syncBooks()
    }

    override fun onPause() {
        super.onPause()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    private fun setupViewModels() {
        val ctx = requireContext().applicationContext
        val database = AppDatabase.getInstance(ctx)
        val bookRepository = BookRepository(database.bookDao())
        val auth = FirebaseAuth.getInstance()
        val userRepository = UserRepository(auth, database.userDao())
        val authRepository = AuthRepository(auth, userRepository)
        viewModel = ViewModelProvider(this, MainViewModel.Factory(bookRepository))[MainViewModel::class.java]
        authViewModel = ViewModelProvider(this, AuthViewModel.Factory(authRepository, userRepository))[AuthViewModel::class.java]
    }

    private fun applyTopInset() {
        val toolbar = binding?.feedToolbar ?: return
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            view.updatePadding(top = top)
            insets
        }
    }

    private fun setupToolbar() {
        binding?.feedProfileButton?.setOnClickListener { view ->
            view.findNavController().navigate(
                FeedFragmentDirections.actionFeedToProfile()
            )
        }
        binding?.feedLogoutButton?.setOnClickListener { view ->
            authViewModel?.signOut()
            view.findNavController().navigate(
                FeedFragmentDirections.actionFeedToLogin()
            )
        }
    }

    private fun setupFab() {
        binding?.createBookFab?.setOnClickListener { view ->
            val action = FeedFragmentDirections.actionFeedToAddEditBook()
            view.findNavController().navigate(action)
        }
    }

    private fun setupSearch() {
        binding?.feedSearchEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel?.setSearchQuery(s?.toString().orEmpty())
            }
        })
    }

    private fun setupRecyclerView() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        adapter = BookAdapter(mutableListOf()).apply {
            ownerNameProvider = { ownerId ->
                when {
                    ownerId == currentUid -> "You"
                    else -> usersById[ownerId]?.takeIf { it.isNotBlank() } ?: "Reader"
                }
            }
            ownerAvatarProvider = { ownerId -> avatarsById[ownerId] }
        }
        binding?.booksRecyclerView?.layoutManager = LinearLayoutManager(context)
        binding?.booksRecyclerView?.adapter = adapter
        binding?.booksRecyclerView?.addOnScrollListener(scrollListener)
    }

    private fun observeBooks() {
        viewModel?.feedBooks?.observe(viewLifecycleOwner) { books ->
            adapter?.submit(books)
        }
        viewModel?.syncResult?.observe(viewLifecycleOwner) { result ->
            binding?.syncProgressBar?.visibility = View.GONE
            if (result is AppResult.Error) {
                Toast.makeText(context, "Sync failed: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeCurrentUser() {
        val btn = binding?.feedProfileButton ?: return
        authViewModel?.currentUser?.observe(viewLifecycleOwner) { user ->
            val url = user?.avatarUrl.orEmpty()
            if (url.isNotBlank()) {
                btn.imageTintList = null
                btn.scaleType = ImageView.ScaleType.CENTER_CROP
                Picasso.get().load(url).fit().centerCrop().into(btn)
            } else {
                btn.imageTintList = ColorStateList.valueOf(Color.WHITE)
                btn.scaleType = ImageView.ScaleType.CENTER_INSIDE
                btn.setImageResource(R.drawable.ic_account)
            }
        }
    }

    private fun observeUsers() {
        authViewModel?.users?.observe(viewLifecycleOwner) { users ->
            usersById.clear()
            avatarsById.clear()
            users.forEach {
                usersById[it.id] = it.name
                avatarsById[it.id] = it.avatarUrl
            }
            // Names may arrive after books were bound — rebind to apply them.
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.booksRecyclerView?.removeOnScrollListener(scrollListener)
        binding = null
        adapter = null
    }

    companion object {
        private const val LOAD_MORE_THRESHOLD = 2
    }
}
