package com.example.bookstore

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.bookstore.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        applySystemBarInsets()
        setupTopBar()
    }

    private fun applySystemBarInsets() {
        val root = binding?.root ?: return
        val toolbar = binding?.topAppBar ?: return
        // Activity root absorbs side + bottom insets so the nav host content respects the gesture
        // nav area. Top is intentionally NOT padded here — it's owned by the toolbar (when visible)
        // or by the active fragment (when the toolbar is hidden, e.g. ProfileFragment).
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            view.updatePadding(top = top)
            insets
        }
    }

    private fun setupTopBar() {
        binding?.topAppBar?.let { setSupportActionBar(it) }
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navController = navHost?.navController
        navController?.let {
            NavigationUI.setupActionBarWithNavController(this, it)
            it.addOnDestinationChangedListener { _, destination, _ ->
                val isProfile = destination.id == R.id.profileFragment
                binding?.topAppBar?.isVisible = !isProfile
                setStatusBarTextLight(isLight = !isProfile)
            }
        }
    }

    private fun setStatusBarTextLight(isLight: Boolean) {
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController?.navigateUp() ?: false || super.onSupportNavigateUp()
    }
}
