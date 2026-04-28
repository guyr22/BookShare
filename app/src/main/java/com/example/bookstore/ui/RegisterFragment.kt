package com.example.bookstore.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.bookstore.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var binding: FragmentRegisterBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRegisterBinding.inflate(layoutInflater, container, false)
        setupView()
        return binding?.root
    }

    private fun setupView() {
        binding?.registerButton?.setOnClickListener {
            val name = binding?.nameEditText?.text.toString()
            val email = binding?.emailEditText?.text.toString()
            val password = binding?.passwordEditText?.text.toString()
            // TODO: wire to AuthViewModel in Sprint 3 — navigate directly for now
            navigateToFeed(it)
        }

        binding?.goToLoginTextView?.setOnClickListener {
            view?.findNavController()?.popBackStack()
        }
    }

    private fun navigateToFeed(view: View) {
        val action = RegisterFragmentDirections.actionRegisterToFeed()
        Navigation.findNavController(view).navigate(action)
    }
}
