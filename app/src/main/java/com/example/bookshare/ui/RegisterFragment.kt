package com.example.bookshare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.bookshare.databinding.FragmentRegisterBinding
import com.example.bookshare.local.AppDatabase
import com.example.bookshare.repository.AppResult
import com.example.bookshare.repository.AuthRepository
import com.example.bookshare.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment() {

    private var binding: FragmentRegisterBinding? = null
    private var authViewModel: AuthViewModel? = null

    /** True once we've kicked off a register; lets us ignore the initial null emission. */
    private var awaitingResult: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRegisterBinding.inflate(layoutInflater, container, false)
        setupViewModel()
        setupView()
        observeAuth()
        return binding?.root
    }

    private fun setupViewModel() {
        val ctx = requireContext().applicationContext
        val authRepository = AuthRepository(
            FirebaseAuth.getInstance(),
            AppDatabase.getInstance(ctx).userDao()
        )
        authViewModel = ViewModelProvider(this, AuthViewModel.Factory(authRepository))[AuthViewModel::class.java]
    }

    private fun setupView() {
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
            authViewModel?.register(email, password, name)
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
