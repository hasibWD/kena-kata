package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.preferences.SessionPreferences
import com.example.data.remote.InsforgeClientProvider
import com.example.data.remote.InsforgeRemoteDataSource
import com.example.data.repository.StoreRepository
import com.example.i18n.AppLanguage
import com.example.ui.navigation.KenaKataApp
import com.example.ui.state.StoreViewModel
import com.example.ui.state.StoreViewModelFactory
import com.example.ui.theme.KenaKataTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val remote = InsforgeClientProvider.getClient(applicationContext)?.let {
            InsforgeRemoteDataSource(applicationContext, it)
        }
        val repository = StoreRepository(db.dao(), remote)
        val preferences = SessionPreferences(applicationContext)
        val factory = StoreViewModelFactory(repository, preferences, remote)

        setContent {
            val vm: StoreViewModel = viewModel(factory = factory)
            val lang by vm.language.collectAsStateWithLifecycle()
            KenaKataTheme(isBengali = lang == AppLanguage.BN) {
                KenaKataApp(vm = vm)
            }
        }
    }
}
