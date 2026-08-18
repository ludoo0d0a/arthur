package fr.geoking.arthur.phone

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.tv.AmbientActivity
import fr.geoking.arthur.ui.screens.ControlPlaneScreen
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val contentEngine: ContentEngine by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            ArthurTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ControlPlaneScreen(
                        contentEngine = contentEngine,
                        onStartAmbient = {
                            startActivity(AmbientActivity.intent(this))
                        },
                    )
                }
            }
        }
    }
}
