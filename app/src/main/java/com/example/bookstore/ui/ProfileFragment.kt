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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookstore.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null

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
        setupView()
        return binding?.root
    }

    private fun setupView() {
        binding?.postsRecyclerView?.layoutManager = LinearLayoutManager(context)
        binding?.postsRecyclerView?.setHasFixedSize(true)

        binding?.avatarEditButton?.setOnClickListener { showImageSourceChooser() }
        binding?.avatarImageView?.setOnClickListener { showImageSourceChooser() }

        binding?.editProfileButton?.setOnClickListener {
            // TODO: wire to AccountViewModel in Sprint 3 — open inline edit form
            Toast.makeText(context, "Edit Profile coming soon", Toast.LENGTH_SHORT).show()
        }
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
}
