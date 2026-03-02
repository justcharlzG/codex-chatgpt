package com.example.repeater

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

data class AudioEntry(
    val name: String,
    val uri: Uri
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RepeaterScreen()
            }
        }
    }
}

@Composable
private fun RepeaterScreen() {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    val audioFiles = remember { mutableStateListOf<AudioEntry>() }
    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }
    var currentTrackName by remember { mutableStateOf("未播放") }

    var speed by remember { mutableFloatStateOf(1.0f) }
    var pointA by remember { mutableLongStateOf(-1L) }
    var pointB by remember { mutableLongStateOf(-1L) }
    var abEnabled by remember { mutableStateOf(false) }
    var loopEnabled by remember { mutableStateOf(false) }

    val handler = remember { Handler(Looper.getMainLooper()) }
    val abRunnable = remember {
        object : Runnable {
            override fun run() {
                if (abEnabled && pointA >= 0L && pointB > pointA && player.isPlaying) {
                    if (player.currentPosition >= pointB) {
                        player.seekTo(pointA)
                    }
                }
                handler.postDelayed(this, 80)
            }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            selectedFolderUri = uri

            val root = DocumentFile.fromTreeUri(context, uri)
            audioFiles.clear()
            root?.listFiles()?.forEach { file ->
                if (file.isFile && file.type?.startsWith("audio/") == true) {
                    audioFiles.add(AudioEntry(file.name ?: "未知文件", file.uri))
                }
            }
        }
    }

    DisposableEffect(Unit) {
        handler.post(abRunnable)

        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentTrackName = mediaItem?.mediaMetadata?.title?.toString() ?: currentTrackName
            }
        }

        player.addListener(listener)
        onDispose {
            handler.removeCallbacks(abRunnable)
            player.removeListener(listener)
            player.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(onClick = { folderPicker.launch(null) }) {
            Text("选择本地文件夹")
        }

        Text(text = "目录: ${selectedFolderUri?.toString() ?: "未选择"}")
        Text(text = "当前播放: $currentTrackName")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                loopEnabled = !loopEnabled
                player.repeatMode = if (loopEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            }) {
                Text(if (loopEnabled) "循环播放: 开" else "循环播放: 关")
            }

            Button(onClick = { pointA = player.currentPosition }) { Text("设A点") }
            Button(onClick = { pointB = player.currentPosition }) { Text("设B点") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { abEnabled = !abEnabled }, enabled = pointA >= 0L && pointB > pointA) {
                Text(if (abEnabled) "AB复读: 开" else "AB复读: 关")
            }
            Button(onClick = {
                pointA = -1L
                pointB = -1L
                abEnabled = false
            }) {
                Text("清空AB")
            }
        }

        Text("A点: ${if (pointA >= 0) pointA / 1000 else "未设置"} 秒")
        Text("B点: ${if (pointB >= 0) pointB / 1000 else "未设置"} 秒")

        Text("播放速度: ${"%.2f".format(speed)}x")
        Slider(
            value = speed,
            onValueChange = {
                speed = it
                player.playbackParameters = PlaybackParameters(speed)
            },
            valueRange = 0.5f..2.0f
        )

        Text("音频文件")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(audioFiles) { item ->
                AudioItemCard(item) {
                    currentTrackName = item.name
                    player.setMediaItem(MediaItem.fromUri(item.uri))
                    player.prepare()
                    player.play()
                }
            }
        }
    }
}

@Composable
private fun AudioItemCard(item: AudioEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(
            text = item.name,
            modifier = Modifier.padding(12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
