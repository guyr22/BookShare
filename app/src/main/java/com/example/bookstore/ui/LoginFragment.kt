package com.example.bookstore.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.bookstore.databinding.FragmentLoginBinding
import com.example.bookstore.local.AppDatabase
import com.example.bookstore.repository.AppResult
import com.example.bookstore.repository.AuthRepository
import com.example.bookstore.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private var binding: FragmentLoginBinding? = null
    private var authViewModel: AuthViewModel? = null

    /** True once we've kicked off a signIn; lets us ignore the initial null emission of authResult. */
    private var awaitingResult: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
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
        binding?.loginButton?.setOnClickListener {
            val email = binding?.emailEditText?.text?.toString()?.trim().orEmpty()
            val password = binding?.passwordEditText?.text?.toString().orEmpty()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding?.loginProgressOverlay?.visibility = View.VISIBLE
            awaitingResult = true
            authViewModel?.signIn(email, password)
        }

        binding?.signUpButton?.setOnClickListener {
            navigateToRegister(it)
        }
    }

    private fun observeAuth() {
        authViewModel?.authResult?.observe(viewLifecycleOwner) { result ->
            if (!awaitingResult || result == null) return@observe
            binding?.loginProgressOverlay?.visibility = View.GONE
            awaitingResult = false
            when (result) {
                is AppResult.Success -> navigateToFeed(requireView())
                is AppResult.Error -> {
                    Toast.makeText(context, "Sign-in failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToFeed(view: View) {
        val action = LoginFragmentDirections.actionLoginToFeed()
        Navigation.findNavController(view).navigate(action)
    }

    private fun navigateToRegister(view: View) {
        val action = LoginFragmentDirections.actionLoginToRegister()
        Navigation.findNavController(view).navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
