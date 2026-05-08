package com.example.bookstore.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import com.example.bookstore.R
import com.example.bookstore.databinding.FragmentFeedBinding
import com.example.bookstore.local.Book
import com.example.bookstore.ui.feed.BookAdapter
import com.example.bookstore.ui.feed.OnBookClickListener

class FeedFragment : Fragment() {

    private var binding: FragmentFeedBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFeedBinding.inflate(layoutInflater, container, false)
        setupMenu()
        return binding?.root
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_feed, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_open_profile -> {
                        view?.let {
                            val action = FeedFragmentDirections.actionFeedToProfile()
                            Navigation.findNavController(it).navigate(action)
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
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
