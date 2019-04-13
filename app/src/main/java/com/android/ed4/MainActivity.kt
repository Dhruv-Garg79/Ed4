package com.android.ed4

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File

class MainActivity : AppCompatActivity(), VideoFileAdapter.OnItemClickListener {

    override fun onItemClickListener(videoFile: File) {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra(VIDEO_URI, videoFile.absolutePath)
        startActivity(intent)
    }

    private val myAdapter = VideoFileAdapter(this)
    private val downloadManager : DownloadManager by lazy {
        getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }
    private val vibrator : Vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private lateinit var PACKAGE_NAME : String
    private val folder = "${Environment.getExternalStorageDirectory()}/Android/data/com.android.ed4/files/Download"

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != null && id != -1L){
                //can do something when download is complete
                Log.d(TAG, id.toString())
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(200)
                }
                getVideoFiles()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        PACKAGE_NAME = packageName

        Log.d(TAG, folder)
        checkPermission()
        getVideoFiles()

        registerReceiver(broadCastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        fabDownload.setOnClickListener {
            downloadVideo(editText.text.toString())
        }
        fabPickPdf.setOnClickListener {
            selectPdf()
        }
    }

    private fun downloadVideo(url : String){
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri)
        request.setDestinationInExternalFilesDir(this, DIRECTORY_DOWNLOADS, "${System.currentTimeMillis()}.mp4")
        request.setTitle(url)

        val id = downloadManager.enqueue(request)
    }

    private fun selectPdf(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        startActivity(intent)
    }

    private fun getVideoFiles(){
        val folder = File(folder)
        val files = folder.listFiles()
        if (!files.isNullOrEmpty())
            myAdapter.updateData(files.toList())
    }

    private fun checkPermission(){
        var result = true
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.VIBRATE
        )
        for (perm in permissions){
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED){
                result = false
            }
        }
        if (!result){
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            PICK_PDF -> {
                val pdfPath : String? = data?.data?.path
                if (pdfPath != null){
                    val pdfUri = Uri.parse(pdfPath)
                    Log.d(TAG, pdfUri.toString())
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){


                }else {
                    Snackbar.make(editText, "Permissions not Granted", LENGTH_SHORT ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(broadCastReceiver)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val VIDEO_URI = "video_uri"
        private const val PICK_PDF = 1
        private const val PERMISSION_CODE = 2
    }
}
