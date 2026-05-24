package com.example.bookstore.ui

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
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookstore.databinding.FragmentFeedBinding
import com.example.bookstore.local.AppDatabase
import com.example.bookstore.local.Book
import com.example.bookstore.repository.AuthRepository
import com.example.bookstore.repository.BookRepository
import com.example.bookstore.ui.feed.BookAdapter
import com.example.bookstore.ui.feed.OnBookClickListener
import com.example.bookstore.viewmodel.AuthViewModel
import com.example.bookstore.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth

class FeedFragment : Fragment() {

    private var binding: FragmentFeedBinding? = null
    private var adapter: BookAdapter? = null
    private var viewModel: MainViewModel? = null
    private var authViewModel: AuthViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFeedBinding.inflate(layoutInflater, container, false)
        applyTopInset()
        setupViewModels()
        setupRecyclerView()
        setupToolbar()
        setupFab()
        observeBooks()
        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
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
                // Until users/{uid} sync lands, show "You" for own books and a generic
                // label for others. The real display name comes from the User table.
                if (ownerId == currentUid) "You" else "Reader"
            }
            listener = object : OnBookClickListener {
                override fun onBookClick(book: Book) {
                    Toast.makeText(context, "Tapped: ${book.title}", Toast.LENGTH_SHORT).show()
                }

                override fun onBookLongClick(book: Book): Boolean {
                    Toast.makeText(context, "Long-pressed: ${book.title}", Toast.LENGTH_SHORT).show()
                    return true
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        adapter = null
    }
}
