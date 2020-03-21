package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.downloader.OnCancelListener
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener {

    val REQUEST_CODE = 1001
    val TAG = "MainActivity"

    val STATUS_DOWNLOADING = 1
    val STATUS_PAUSED = 2
    val STATUS_CANCELLED = 3
    val STATUS_ERROR = 4

    var currentStatus = 0

    val DOWNLOAD_SYNC = 10
    val DOWNLOAD_ASYNC = 11
    var downloadState = DOWNLOAD_ASYNC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        isStoragePermissionGranted()
        initDownloader()
        initView()
        loadImage()
    }

    private fun initDownloader() {
        // Enabling database for resume support even after the application is killed:
        // Enabling database for resume support even after the application is killed:
        val config = PRDownloaderConfig.newBuilder()
            .setDatabaseEnabled(true)
            .build()
        PRDownloader.initialize(applicationContext, config)
    }

    private fun loadImage() {
        Glide.with(this).load(getString(R.string.image_link_1)).into(img1)
        Glide.with(this).load(getString(R.string.image_link_2)).into(img2)
        Glide.with(this).load(getString(R.string.image_link_3)).into(img3)
        Glide.with(this).load(getString(R.string.image_link_4)).into(img4)
    }

    fun isStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                return true;
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE
                );
                return false;
            }
        } else {
            return true;
        }
    }

    private fun initView() {
        txtSync.setOnClickListener(this)
        txtAsync.setOnClickListener(this)
        btnDownload.setOnClickListener(this)
        selected(DOWNLOAD_SYNC)
    }

    fun selected(value: Int) {
        when (value) {
            DOWNLOAD_SYNC -> {
                txtSync.isSelected = true
                txtAsync.isSelected = false
                txtSync.setBackgroundResource(R.drawable.bg_selected)
                txtAsync.setBackgroundResource(R.drawable.bg_unselected)
            }
            DOWNLOAD_ASYNC -> {
                txtSync.isSelected = false
                txtAsync.isSelected = true
                txtSync.setBackgroundResource(R.drawable.bg_unselected)
                txtAsync.setBackgroundResource(R.drawable.bg_selected)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.txtSync -> {
                downloadState == DOWNLOAD_SYNC
                selected(DOWNLOAD_SYNC)
            }
            R.id.txtAsync -> {
                downloadState == DOWNLOAD_ASYNC
                selected(DOWNLOAD_ASYNC)
            }
            R.id.btnDownload -> {
                Log.d(TAG, "downloadState -> "+downloadState)
                if (downloadState == DOWNLOAD_ASYNC) {
                    downloadAsyncronusly()
                } else {
                    downloadSyncronusly()
                }
            }
        }
    }

    fun downloadSyncronusly() {
        CoroutineScope(Dispatchers.Default).launch() {
            getBitmapAndSaveSync(
                getString(R.string.image_link_1),
                getDirectoryPath()!!,
                createImageFile(1)!!,
                progress1
            )

            getBitmapAndSaveSync(
                getString(R.string.image_link_2),
                getDirectoryPath()!!,
                createImageFile(2)!!,
                progress2
            )

            getBitmapAndSaveSync(
                getString(R.string.image_link_3),
                getDirectoryPath()!!,
                createImageFile(3)!!,
                progress3
            )

            getBitmapAndSaveSync(
                getString(R.string.image_link_4),
                getDirectoryPath()!!,
                createImageFile(4)!!,
                progress4
            )
        }
    }

    fun downloadAsyncronusly() {
        getBitmapAndSave(
            getString(R.string.image_link_1),
            getDirectoryPath()!!,
            createImageFile(1)!!,
            progress1
        )

        getBitmapAndSave(
            getString(R.string.image_link_2),
            getDirectoryPath()!!,
            createImageFile(2)!!,
            progress2
        )

        getBitmapAndSave(
            getString(R.string.image_link_3),
            getDirectoryPath()!!,
            createImageFile(3)!!,
            progress3
        )

        getBitmapAndSave(
            getString(R.string.image_link_4),
            getDirectoryPath()!!,
            createImageFile(4)!!,
            progress4
        )
    }

    private fun getDirectoryPath(): String? {
//        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()/* + "/Sample"*/
        val dir = Environment.getDownloadCacheDirectory().toString()
        val dirFile = File(dir)
        if (!dirFile.exists())
            dirFile.mkdirs()
        return dirFile.absolutePath
    }

    private fun createImageFile(value: Int): String? {
        val timeStamp: String = SimpleDateFormat("HHmmss").format(Date())
        val imageFileName = "AppName_$timeStamp$value"

        val file = File(this@MainActivity.filesDir, "image")
        if (!file.exists()) {
            file.mkdir()
        }
        val imageFile = File(file, imageFileName + ".jpg")
        Log.d(TAG, "createImageFile -> " + imageFile)
        return imageFile.absolutePath
    }


    fun getBitmapAndSave(
        url: String,
        path: String,
        fileName: String,
        progressBar: ProgressBar
    ) {
        Log.d(TAG, "getBitmapAndSave called")
        progressBar.visibility = View.VISIBLE
        val downloadId = PRDownloader.download(url, path, fileName)
            .build()
            .setOnStartOrResumeListener {
                currentStatus = STATUS_DOWNLOADING
            }
            .setOnPauseListener {
                currentStatus = STATUS_PAUSED
            }
            .setOnCancelListener(object : OnCancelListener {
                override fun onCancel() {
                    currentStatus = STATUS_CANCELLED
                    Log.e(TAG, "onCancel executed")
                }
            })
            .setOnProgressListener { progress ->
                val value = (progress.currentBytes / progress.totalBytes) * 100
                progressBar.progress = value.toInt()
                Log.d(TAG, "OnProgress -> " + value)
            }
            .start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    progressBar.visibility = View.GONE
                }

                override fun onError(error: com.downloader.Error?) {
                    currentStatus = STATUS_ERROR
                    error?.connectionException?.printStackTrace()
                    Log.e(TAG, error?.serverErrorMessage + ", " + error?.isConnectionError + ", ")
                }
            })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission Granted")
        }
    }

    suspend fun getBitmapAndSaveSync(
        url: String,
        path: String,
        fileName: String,
        progressBar: ProgressBar
    ) = suspendCancellableCoroutine<Unit> { cont ->

        cont.invokeOnCancellation { cont.cancel() }
        Log.d(TAG, "getBitmapAndSaveSync called")
        progressBar.visibility = View.VISIBLE
        val downloadId = PRDownloader.download(url, path, fileName)
            .build()
            .setOnStartOrResumeListener {
                currentStatus = STATUS_DOWNLOADING
            }
            .setOnPauseListener {
                currentStatus = STATUS_PAUSED
            }
            .setOnCancelListener(object : OnCancelListener {
                override fun onCancel() {
                    currentStatus = STATUS_CANCELLED
                    Log.e(TAG, "onCancel executed")
                }
            })
            .setOnProgressListener { progress ->
                val value = (progress.currentBytes / progress.totalBytes) * 100
                progressBar.progress = value.toInt()
                Log.d(TAG, "OnProgress -> " + value)
            }
            .start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    progressBar.visibility = View.GONE
                    cont.resume(Unit, {})
                }

                override fun onError(error: com.downloader.Error?) {
                    currentStatus = STATUS_ERROR
                    error?.connectionException?.printStackTrace()
                    Log.e(TAG, error?.serverErrorMessage + ", " + error?.isConnectionError + ", ")
                }
            })
    }
}
