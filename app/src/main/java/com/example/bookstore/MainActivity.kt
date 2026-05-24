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
import com.example.bookstore.local.AppDatabase
import com.example.bookstore.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth

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
        navController?.let { controller ->
            applyAutoLogin(controller)
            NavigationUI.setupActionBarWithNavController(this, controller)
            controller.addOnDestinationChangedListener { _, destination, _ ->
                val isProfile = destination.id == R.id.profileFragment
                binding?.topAppBar?.isVisible = !isProfile
                setStatusBarTextLight(isLight = !isProfile)
            }
        }
    }

    /**
     * If Firebase already has a signed-in user, swap the nav graph's start
     * destination to Feed so returning users never see the Login screen.
     * Called before NavigationUI.setupActionBarWithNavController so the
     * ActionBar's "up" arrow logic uses the correct start destination.
     */
    private fun applyAutoLogin(controller: NavController) {
        val authRepository = AuthRepository(
            FirebaseAuth.getInstance(),
            AppDatabase.getInstance(this).userDao()
        )
        if (!authRepository.isLoggedIn()) return

        val graph = controller.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(R.id.feedFragment)
        controller.graph = graph
    }

    private fun setStatusBarTextLight(isLight: Boolean) {
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController?.navigateUp() ?: false || super.onSupportNavigateUp()
    }
}
