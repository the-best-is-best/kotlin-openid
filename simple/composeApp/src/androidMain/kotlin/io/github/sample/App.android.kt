package io.github.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.kmmcrypto.AndroidKMMCrypto
import io.github.openid.AndroidOpenId

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AndroidOpenId.init(this)
        AndroidKMMCrypto.init(this, "key0")
        setContent { App() }
    }


}

@Preview
@Composable
fun AppPreview() { App() }
