package io.github.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.openid.AndroidOpenId
import kmmopenid.simple.composeapp.generated.resources.Res

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AndroidOpenId.init(this)
        setContent { App() }
    }


}

@Preview
@Composable
fun AppPreview() { App() }
