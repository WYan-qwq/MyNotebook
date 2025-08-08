package com.example.mynotebook

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.mynotebook.plan.PlanActivity
import com.example.mynotebook.ui.theme.MyNotebookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyNotebookTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    // 拿到当前 Context
    val ctx = LocalContext.current
    Button(onClick = {
        // 显式 Intent 进入 PlanActivity
        ctx.startActivity(
            Intent(ctx, PlanActivity::class.java)
        )
    }) {
        Text("查看我的计划")
    }
}