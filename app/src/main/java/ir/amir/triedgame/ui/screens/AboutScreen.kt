package ir.amir.triedgame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("درباره ما", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(
            "تریدر شو یک بازی شبیه‌سازی ترید است که به شما اجازه می‌دهد بدون ریسک پول " +
                "واقعی، دنیای بازارهای مالی را تجربه کنید. از تحلیل چارت و باز کردن " +
                "پوزیشن تا مدیریت زندگی روزمره‌تان با سودی که به دست می‌آورید — همه‌چیز " +
                "شبیه واقعیت است، فقط پولش مجازی است.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
