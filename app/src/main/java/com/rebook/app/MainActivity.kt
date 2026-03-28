package com.rebook.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import com.rebook.app.data.repository.UserRepository
import com.rebook.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Set the nav graph dynamically to handle auto-login start destination
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(
            if (FirebaseAuth.getInstance().currentUser != null) R.id.mainFragment
            else R.id.loginFragment
        )
        navController.graph = navGraph

        if (FirebaseAuth.getInstance().currentUser != null) {
            lifecycleScope.launch {
                UserRepository(applicationContext).syncUserDocumentFromAuth()
            }
        }
    }
}
