package com.example.bookstore.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.bookstore.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var binding: FragmentLoginBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        setupView()
        return binding?.root
    }

    private fun setupView() {
        binding?.loginButton?.setOnClickListener {
            val email = binding?.emailEditText?.text.toString()
            val password = binding?.passwordEditText?.text.toString()
            // TODO: wire to AuthViewModel in Sprint 3 — navigate directly for now
            navigateToFeed(it)
        }

        binding?.signUpButton?.setOnClickListener {
            navigateToRegister(it)
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
}
