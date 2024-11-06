package com.example.hotwordswakeupdemo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anysou.aslogger.ASLogFileUtils
import com.anysou.aslogger.ASLogger
import com.example.hotwordswakeupdemo.ui.theme.HotWordsWakeupDemoTheme
import edu.cmu.pocketsphinx.PocketListener
import edu.cmu.pocketsphinx.PocketSphinxService
import edu.cmu.pocketsphinx.PocketSphinxUtil
import edu.cmu.pocketsphinx.kit.RecognizerSetupListener
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.delay
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import kotlin.math.log

class MainActivity : ComponentActivity() {

    private var serviceIntent: Intent? = null
    private var pocketSphinxUtil: PocketSphinxUtil? = null
    private  val TAG: String = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            HotWordsWakeupDemoTheme {
                MainScreen(
                    onStart = { startRecord() },
                    onStop = { stopRecord() },
                    log = ASLogFileUtils.readLog()
                )
            }
        }

        // 执行录音权限及服务启动
        onRecordAudioGranted()
        startPocketSphinxService()
    }
    private fun startRecord() {
        // 开始操作
        Handler(Looper.getMainLooper())
            .postDelayed({ this.forTest() }, 500)
    }

    private fun stopRecord() {
        // 停止操作
        if (pocketSphinxUtil != null) {
            pocketSphinxUtil!!.stopRecord()
        }
    }

    private fun forTest() {
        pocketSphinxUtil = PocketSphinxUtil.get();
        if (pocketSphinxUtil == null) {
            return;
        }
        pocketSphinxUtil!!.runRecognizerSetup(object : RecognizerSetupListener {

            override fun onRecognizerAlreadySetup() {

            }

            override fun doInBackGround(): Exception {
                return Exception("Some error occurred")
            }

            override fun onRecognizerPrepareError() {
                ASLogger.i(TAG,"onRecognizerPrepareError")
            }

            override fun onRecognizerPrepareSuccess() {
                ASLogger.i(TAG,"onRecognizerPrepareSuccess")
            }
        });

        pocketSphinxUtil!!.startRecord("zh_test", object: PocketListener {
            override fun onSpeechStart() {

            }

            override fun onSpeechResult(strings: MutableList<String>?) {
                ASLogger.i("MainActivity", "find result =" + strings);
                if (strings == null) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "没有监听到关键词", Toast.LENGTH_LONG).show();
                    }
                    return;
                }
                for ( value in strings) {
                    ASLogger.i("MainActivity", "find string =" + value);
                }
                runOnUiThread {
//                    Toast.makeText(this@MainActivity, strings[0], Toast.LENGTH_LONG).show();
                    Toasty.success(this@MainActivity, strings[0], Toast.LENGTH_SHORT, true).show();
                }
            }

            override fun onSpeechError(error: String?) {

            }
        });
    }

    private fun hasRecordAudioPermission(): Boolean {
        return EasyPermissions.hasPermissions(this, Manifest.permission.RECORD_AUDIO)
    }

    @AfterPermissionGranted(100)
    private fun onRecordAudioGranted() {
        if (hasRecordAudioPermission()) {
            Toast.makeText(this, "已获取录音权限", Toast.LENGTH_SHORT).show()
        } else {
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, 100, Manifest.permission.RECORD_AUDIO)
                    .build()
            )
        }
    }

    private fun startPocketSphinxService() {
        serviceIntent = Intent()
        serviceIntent!!.setClass(this, PocketSphinxService::class.java)
        startService(serviceIntent)
        Toast.makeText(this, "PocketSphinxService 启动成功", Toast.LENGTH_SHORT).show()
    }



    override fun onPause() {
        super.onPause()
        if(pocketSphinxUtil !=null){
            pocketSphinxUtil!!.stopRecord()
        }
    }

    override fun onStop() {
        super.onStop()
        if(serviceIntent!=null){
            stopService(serviceIntent)
        }
    }
}

@Composable
fun MainScreen(onStart: () -> Unit, onStop: () -> Unit, log: String) {

    // 维护一个文本状态，保存日志内容
    var logText by remember { mutableStateOf("") }
    // 定期更新日志内容
    LaunchedEffect(Unit) {
        while (true) {
            logText = ASLogFileUtils.readLog() // 读取最新的日志
            delay(1000) // 每秒更新一次日志，间隔可以根据需要调整
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            // 添加 Text 组件，设置固定高度并启用滚动
            // 使用 LazyColumn 来显示日志内容并确保底部可见
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(min = 100.dp, max = 200.dp) // 设置固定高度范围
            ) {
                // 显示单行日志内容
                item {
                    Text(
                        text = logText, // 显示更新后的日志
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onStart() },
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(0.5f)
            ) {
                Text("开始")
            }

            Button(
                onClick = { onStop() },
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(0.5f)
            ) {
                Text("结束")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HotWordsWakeupDemoTheme {
        MainScreen(
            onStart = { /* 开始按钮的测试逻辑 */ },
            onStop = { /* 停止按钮的测试逻辑 */ },
            log = "这是一个测试日志"
        )
    }
}


