package com.example.bookstore.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.databinding.FragmentProfileBinding
import com.example.bookstore.local.AppDatabase
import com.example.bookstore.local.Book
import com.example.bookstore.repository.AuthRepository
import com.example.bookstore.repository.BookRepository
import com.example.bookstore.ui.profile.OnReviewClickListener
import com.example.bookstore.ui.profile.ReviewsAdapter
import com.example.bookstore.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null
    private var adapter: ReviewsAdapter? = null
    private var viewModel: MainViewModel? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            binding?.avatarImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
            binding?.avatarImageView?.setImageBitmap(bitmap)
        } else {
            Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            binding?.avatarImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
            Picasso.get()
                .load(uri)
                .fit()
                .centerCrop()
                .into(binding?.avatarImageView)
        } else {
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        applyTopInset()
        setupViewModel()
        setupView()
        setupReviewsList()
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
        val toolbar = binding?.profileToolbar ?: return
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            view.updatePadding(top = top + dp(14))
            insets
        }
    }

    private fun setupView() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        binding?.nameTextView?.text = currentUser?.displayName
            ?: currentUser?.email?.substringBefore("@")
                    ?: "Your Name"

        binding?.backButton?.setOnClickListener {
            it.findNavController().popBackStack()
        }
        binding?.logoutButton?.setOnClickListener { view ->
            val ctx = requireContext().applicationContext
            val authRepository = AuthRepository(
                FirebaseAuth.getInstance(),
                AppDatabase.getInstance(ctx).userDao()
            )
            authRepository.signOut()
            view.findNavController().navigate(
                ProfileFragmentDirections.actionProfileToLogin()
            )
        }
        binding?.avatarImageView?.setOnClickListener { showImageSourceChooser() }
        binding?.editProfileButton?.setOnClickListener {
            // TODO: open edit-profile form when AccountViewModel ships in Sprint 3
            Toast.makeText(context, "Edit Profile coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupReviewsList() {
        val ctx = requireContext()
        val displayName = binding?.nameTextView?.text?.toString()

        adapter = ReviewsAdapter(mutableListOf()).apply {
            this.displayName = displayName
            listener = object : OnReviewClickListener {
                override fun onReviewClick(book: Book) {
                    val action = ProfileFragmentDirections.actionProfileToAddEditBook(book.id)
                    view?.findNavController()?.navigate(action)
                }
            }
        }
        binding?.reviewsRecyclerView?.layoutManager = LinearLayoutManager(ctx)
        binding?.reviewsRecyclerView?.adapter = adapter

        attachSwipeToDelete()

        val ownerId = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerId == null) {
            binding?.emptyStateTextView?.visibility = View.VISIBLE
            return
        }
        viewModel?.getBooksByOwner(ownerId)?.observe(viewLifecycleOwner) { books ->
            adapter?.submit(books)
            binding?.emptyStateTextView?.visibility =
                if (books.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun attachSwipeToDelete() {
        val recyclerView = binding?.reviewsRecyclerView ?: return
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val book = adapter?.items?.getOrNull(position) ?: return
                viewModel?.deleteBook(book)
                // The LiveData observer refreshes the list once Room confirms the delete.
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun showImageSourceChooser() {
        val options = arrayOf("Take a photo", "Choose from gallery")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change profile photo")
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
