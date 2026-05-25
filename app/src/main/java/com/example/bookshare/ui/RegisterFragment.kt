package com.example.bookshare.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.bookshare.databinding.FragmentRegisterBinding
import com.example.bookshare.local.AppDatabase
import com.example.bookshare.repository.AppResult
import com.example.bookshare.repository.AuthRepository
import com.example.bookshare.repository.UserRepository
import com.example.bookshare.viewmodel.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment() {

    private var binding: FragmentRegisterBinding? = null
    private var authViewModel: AuthViewModel? = null

    private var pendingAvatarBitmap: Bitmap? = null

    /** True once we've kicked off a register; lets us ignore the initial null emission. */
    private var awaitingResult: Boolean = false

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) stageAvatar(bitmap)
        else Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = decodeBitmap(uri)
            if (bitmap != null) stageAvatar(bitmap)
            else Toast.makeText(context, "Couldn't read that image", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stageAvatar(bitmap: Bitmap) {
        pendingAvatarBitmap = bitmap
        binding?.avatarImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
        binding?.avatarImageView?.setImageBitmap(bitmap)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRegisterBinding.inflate(layoutInflater, container, false)
        setupViewModel()
        setupView()
        observeAuth()
        return binding?.root
    }

    private fun setupViewModel() {
        val ctx = requireContext().applicationContext
        val auth = FirebaseAuth.getInstance()
        val userDao = AppDatabase.getInstance(ctx).userDao()
        val userRepository = UserRepository(auth, userDao)
        val authRepository = AuthRepository(auth, userRepository)
        authViewModel = ViewModelProvider(this, AuthViewModel.Factory(authRepository, userRepository))[AuthViewModel::class.java]
    }

    private fun setupView() {
        binding?.avatarContainer?.setOnClickListener { showImageSourceChooser() }

        binding?.registerButton?.setOnClickListener {
            val name = binding?.nameEditText?.text?.toString()?.trim().orEmpty()
            val email = binding?.emailEditText?.text?.toString()?.trim().orEmpty()
            val password = binding?.passwordEditText?.text?.toString().orEmpty()
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Name, email and password are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding?.registerProgressOverlay?.visibility = View.VISIBLE
            awaitingResult = true
            authViewModel?.register(email, password, name, pendingAvatarBitmap)
        }

        binding?.goToLoginTextView?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeAuth() {
        authViewModel?.authResult?.observe(viewLifecycleOwner) { result ->
            if (!awaitingResult || result == null) return@observe
            binding?.registerProgressOverlay?.visibility = View.GONE
            awaitingResult = false
            when (result) {
                is AppResult.Success -> navigateToFeed(requireView())
                is AppResult.Error -> {
                    Toast.makeText(context, "Register failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToFeed(view: View) {
        val action = RegisterFragmentDirections.actionRegisterToFeed()
        Navigation.findNavController(view).navigate(action)
    }

    private fun showImageSourceChooser() {
        val options = arrayOf("Take a photo", "Choose from gallery")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add profile photo")
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
