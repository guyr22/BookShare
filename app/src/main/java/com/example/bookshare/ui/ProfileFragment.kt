package com.example.bookshare.ui

import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.example.bookshare.R
import com.example.bookshare.databinding.FragmentProfileBinding
import com.example.bookshare.local.AppDatabase
import com.example.bookshare.model.Book
import com.example.bookshare.model.User
import com.example.bookshare.repository.AppResult
import com.example.bookshare.repository.AuthRepository
import com.example.bookshare.repository.BookRepository
import com.example.bookshare.ui.profile.OnReviewClickListener
import com.example.bookshare.ui.profile.ReviewsAdapter
import com.example.bookshare.viewmodel.AuthViewModel
import com.example.bookshare.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null
    private var adapter: ReviewsAdapter? = null
    private var viewModel: MainViewModel? = null
    private var authViewModel: AuthViewModel? = null

    /** Avatar chosen but not yet persisted — uploaded only when the user taps Save. */
    private var pendingAvatarBitmap: Bitmap? = null

    /** Live reference to the avatar ImageView inside the edit-profile dialog, if open. */
    private var dialogAvatarView: ImageView? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            stageAvatar(bitmap)
        } else {
            Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = decodeBitmap(uri)
            if (bitmap != null) {
                stageAvatar(bitmap)
            } else {
                Toast.makeText(context, "Couldn't read that image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    /** Previews a picked avatar and holds it until the user saves the profile. */
    private fun stageAvatar(bitmap: Bitmap) {
        pendingAvatarBitmap = bitmap
        binding?.avatarImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
        binding?.avatarImageView?.setImageBitmap(bitmap)
        dialogAvatarView?.scaleType = ImageView.ScaleType.CENTER_CROP
        dialogAvatarView?.setImageBitmap(bitmap)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        applyTopInset()
        setupViewModels()
        setupView()
        setupReviewsList()
        observeCurrentUser()
        observeUpdateProfile()
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
        // Initial fallback name from FirebaseAuth — replaced as soon as the Room
        // currentUser observer fires with the canonical record.
        val firebaseUser = authViewModel?.firebaseUser
        binding?.nameTextView?.text = firebaseUser?.displayName
            ?: firebaseUser?.email?.substringBefore("@")
                    ?: "Your Name"

        binding?.backButton?.setOnClickListener {
            it.findNavController().popBackStack()
        }
        binding?.logoutButton?.setOnClickListener { view ->
            authViewModel?.signOut()
            view.findNavController().navigate(
                ProfileFragmentDirections.actionProfileToLogin()
            )
        }
        binding?.avatarImageView?.setOnClickListener { showImageSourceChooser() }
        binding?.editProfileButton?.setOnClickListener { showEditProfileDialog() }
    }

    private fun showEditProfileDialog() {
        val ctx = requireContext()
        val currentName = binding?.nameTextView?.text?.toString().orEmpty()

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val avatarContainer = dialogView.findViewById<View>(R.id.dialogAvatarContainer)
        val avatarImage = dialogView.findViewById<ImageView>(R.id.dialogAvatarImage)
        val input = dialogView.findViewById<EditText>(R.id.dialogNameInput)

        input.setText(currentName)
        input.setSelection(currentName.length)

        val staged = pendingAvatarBitmap
        if (staged != null) {
            avatarImage.scaleType = ImageView.ScaleType.CENTER_CROP
            avatarImage.setImageBitmap(staged)
        } else {
            val avatarUrl = authViewModel?.currentUser?.value?.avatarUrl.orEmpty()
            if (avatarUrl.isNotBlank()) {
                Picasso.get().load(avatarUrl).fit().centerCrop().into(avatarImage)
            }
        }

        dialogAvatarView = avatarImage
        avatarContainer.setOnClickListener { showImageSourceChooser() }

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.profile_edit_title)
            .setView(dialogView)
            .setNegativeButton(R.string.profile_edit_cancel, null)
            .setPositiveButton(R.string.profile_edit_save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    Toast.makeText(ctx, R.string.profile_edit_name_required, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()
                binding?.saveProgressOverlay?.visibility = View.VISIBLE
                authViewModel?.updateProfile(name, pendingAvatarBitmap)
            }
        }

        dialog.setOnDismissListener { dialogAvatarView = null }

        dialog.show()
    }

    private fun observeUpdateProfile() {
        authViewModel?.updateProfileResult?.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            binding?.saveProgressOverlay?.visibility = View.GONE
            when (result) {
                is AppResult.Success -> {
                    // Persisted — drop the staged bitmap; the currentUser observer
                    // refreshes the header (name + avatar) from Room.
                    pendingAvatarBitmap = null
                    Toast.makeText(context, R.string.profile_edit_success, Toast.LENGTH_SHORT).show()
                }
                is AppResult.Error -> Toast.makeText(
                    context,
                    getString(R.string.profile_edit_failed, result.message),
                    Toast.LENGTH_LONG
                ).show()
            }
            authViewModel?.clearUpdateProfileResult()
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return try {
            val resolver = requireContext().contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(resolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    // Force a software, mutable bitmap so it can be JPEG-compressed for upload.
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

    private fun observeCurrentUser() {
        authViewModel?.currentUser?.observe(viewLifecycleOwner) { user ->
            applyUserToHeader(user)
            adapter?.displayName = user?.name?.takeIf { it.isNotBlank() }
                ?: binding?.nameTextView?.text?.toString()
            adapter?.avatarUrl = user?.avatarUrl?.takeIf { it.isNotBlank() }
            adapter?.notifyDataSetChanged()
        }
    }

    private fun applyUserToHeader(user: User?) {
        if (user != null && user.name.isNotBlank()) {
            binding?.nameTextView?.text = user.name
        }
        val avatar = binding?.avatarImageView ?: return
        val avatarUrl = user?.avatarUrl.orEmpty()
        if (avatarUrl.isNotBlank()) {
            avatar.scaleType = ImageView.ScaleType.CENTER_CROP
            Picasso.get()
                .load(avatarUrl)
                .fit()
                .centerCrop()
                .into(avatar)
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
