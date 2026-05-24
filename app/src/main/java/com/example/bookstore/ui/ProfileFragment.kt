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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookstore.R
import com.example.bookstore.databinding.FragmentProfileBinding
import com.example.bookstore.local.AppDatabase
import com.example.bookstore.repository.AuthRepository
import com.example.bookstore.ui.profile.Review
import com.example.bookstore.ui.profile.ReviewsAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null
    private var adapter: ReviewsAdapter? = null

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
        setupView()
        setupReviewsList()
        return binding?.root
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
        binding?.reviewsRecyclerView?.layoutManager = LinearLayoutManager(context)
        val reviews = mockReviews().toMutableList()
        adapter = ReviewsAdapter(reviews)
        binding?.reviewsRecyclerView?.adapter = adapter
        binding?.emptyStateTextView?.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun mockReviews(): List<Review> = listOf(
        Review(
            authorName = "Alex Reader",
            bookTitle = "Project Hail Mary",
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor.",
            coverBackgroundRes = R.drawable.bg_book_cover_dark
        ),
        Review(
            authorName = "Alex Reader",
            bookTitle = "Klara and the Sun",
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor.",
            coverBackgroundRes = R.drawable.bg_book_cover_red
        )
    )

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
