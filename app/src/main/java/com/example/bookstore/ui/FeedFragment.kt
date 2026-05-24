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
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookstore.databinding.FragmentFeedBinding
import com.example.bookstore.local.AppDatabase
import com.example.bookstore.local.Book
import com.example.bookstore.repository.AuthRepository
import com.example.bookstore.ui.feed.BookAdapter
import com.example.bookstore.ui.feed.OnBookClickListener
import com.google.firebase.auth.FirebaseAuth

class FeedFragment : Fragment() {

    private var binding: FragmentFeedBinding? = null
    private var adapter: BookAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFeedBinding.inflate(layoutInflater, container, false)
        applyTopInset()
        setupRecyclerView()
        setupToolbar()
        setupFab()
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
            val ctx = requireContext().applicationContext
            val authRepository = AuthRepository(
                FirebaseAuth.getInstance(),
                AppDatabase.getInstance(ctx).userDao()
            )
            authRepository.signOut()
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
        adapter = BookAdapter(mockBooks().toMutableList()).apply {
            ownerNameProvider = { ownerId -> mockOwnerNames[ownerId] }
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        adapter = null
    }

    private val mockOwnerNames = mapOf(
        "u_alex" to "Alex Reader",
        "u_morgan" to "Morgan Quill",
        "u_sam" to "Sam Pages"
    )

    private fun mockBooks(): List<Book> = listOf(
        Book(
            id = "mock-1",
            title = "Project Hail Mary",
            author = "Andy Weir",
            description = "A lone astronaut wakes up light-years from Earth with no memory and the future of humanity riding on whatever he decides to do next.",
            coverUrl = "",
            ownerId = "u_alex"
        ),
        Book(
            id = "mock-2",
            title = "Klara and the Sun",
            author = "Kazuo Ishiguro",
            description = "An Artificial Friend with outstanding observational qualities watches the world from a store window and hopes for a customer to choose her.",
            coverUrl = "",
            ownerId = "u_morgan"
        ),
        Book(
            id = "mock-3",
            title = "The Three-Body Problem",
            author = "Liu Cixin",
            description = "A secret military project sends signals into space and an alien civilization on the brink of destruction sets a plan in motion.",
            coverUrl = "",
            ownerId = "u_sam"
        )
    )
}
