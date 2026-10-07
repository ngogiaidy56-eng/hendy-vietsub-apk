package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SubCutDatabase
import com.example.data.SubCutRepository
import com.example.ui.screens.SubCutStudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract text or URL if launched from another app via "Dịch bằng Hendy Vietsub" (PROCESS_TEXT) or Share (SEND)
        val externalText = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }

        setContent {
            val repository = remember {
                val db = SubCutDatabase.getInstance(applicationContext)
                SubCutRepository(db.subCutDao())
            }
            val studioViewModel: StudioViewModel = viewModel(
                factory = StudioViewModel.Factory(repository)
            )
            MyApplicationTheme {
                SubCutStudioScreen(
                    viewModel = studioViewModel,
                    initialExternalTextToTranslate = externalText
                )
            }
        }
    }
}
