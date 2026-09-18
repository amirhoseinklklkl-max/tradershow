package ir.amir.triedgame

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ir.amir.triedgame.ads.AdManager
import ir.amir.triedgame.billing.BillingManager
import ir.amir.triedgame.data.GameRepository
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.GameViewModelFactory
import ir.amir.triedgame.ui.navigation.MainNavGraph
import ir.amir.triedgame.ui.screens.RegistrationScreen
import ir.amir.triedgame.ui.theme.TraderShowTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: GameRepository
    private var adManager: AdManager? = null
    private var billingManager: BillingManager? = null

    private val viewModel: GameViewModel by viewModels {
        GameViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = GameRepository(applicationContext)
        adManager = AdManager(this, repository).also { it.prepare() }
        billingManager = BillingManager(
            activity = this,
            onWalletTopUp = { usd -> viewModel.creditUsd(usd) }
        )
        billingManager?.start()

        viewModel.loadOrCreateProfile(repository.loadProfile())

        setContent {
            TraderShowTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val profile = viewModel.profile
                    if (profile == null) {
                        RegistrationScreen(onRegister = { first, last ->
                            viewModel.registerNewUser(first, last)
                        })
                    } else {
                        MainNavGraph(viewModel, adManager, billingManager)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        billingManager?.dispose()
        super.onDestroy()
    }
}
