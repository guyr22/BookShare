package com.example.bookshare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.bookshare.databinding.FragmentFeedBinding
import com.example.bookshare.local.AppDatabase
import com.example.bookshare.repository.AppResult
import com.example.bookshare.repository.AuthRepository
import com.example.bookshare.repository.BookRepository
import com.example.bookshare.ui.feed.BookAdapter
import com.example.bookshare.viewmodel.AuthViewModel
import com.example.bookshare.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth

class FeedFragment : Fragment() {

    private var binding: FragmentFeedBinding? = null
    private var adapter: BookAdapter? = null
    private var viewModel: MainViewModel? = null
    private var authViewModel: AuthViewModel? = null

    /** ownerId → display name, populated from the synced user cache. */
    private val usersById = mutableMapOf<String, String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFeedBinding.inflate(layoutInflater, container, false)
        applyTopInset()
        setupViewModels()
        setupRecyclerView()
        setupToolbar()
        setupFab()
        observeBooks()
        observeUsers()
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
        val authRepository = AuthRepository(FirebaseAuth.getInstance(), database.userDao())
        viewModel = ViewModelProvider(this, MainViewModel.Factory(bookRepository))[MainViewModel::class.java]
        authViewModel = ViewModelProvider(this, AuthViewModel.Factory(authRepository))[AuthViewModel::class.java]
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

    private fun setupRecyclerView() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        adapter = BookAdapter(mutableListOf()).apply {
            ownerNameProvider = { ownerId ->
                // Own posts read "You"; everyone else shows their synced display name,
                // falling back to "Reader" until their user record arrives.
                when {
                    ownerId == currentUid -> "You"
                    else -> usersById[ownerId]?.takeIf { it.isNotBlank() } ?: "Reader"
                }
            }
        }
        binding?.booksRecyclerView?.layoutManager = LinearLayoutManager(context)
        binding?.booksRecyclerView?.adapter = adapter
    }

    private fun observeBooks() {
        viewModel?.allBooks?.observe(viewLifecycleOwner) { books ->
            adapter?.submit(books)
        }
        viewModel?.syncResult?.observe(viewLifecycleOwner) { result ->
            binding?.syncProgressBar?.visibility = View.GONE
            if (result is AppResult.Error) {
                Toast.makeText(context, "Sync failed: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeUsers() {
        authViewModel?.users?.observe(viewLifecycleOwner) { users ->
            usersById.clear()
            users.forEach { usersById[it.id] = it.name }
            // Names may arrive after the books were bound — re-bind to apply them.
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        adapter = null
    }
}
