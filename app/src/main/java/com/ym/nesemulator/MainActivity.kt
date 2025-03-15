package com.ym.nesemulator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ym.library.sdk.EmulatorManager
import java.io.File
import java.io.FileFilter

class MainActivity : AppCompatActivity() {
    private val recyclerView: RecyclerView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        findViewById(R.id.rv)
    }

    private val tvEmpty: TextView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        findViewById(R.id.tvEmpty)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUp()
    }

    private fun setUp() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
            adapter = object : Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val itemView = layoutInflater.inflate(R.layout.item_game, parent, false)
                    return object : ViewHolder(itemView) {}
                }

                override fun getItemCount(): Int {
                    return Scanner.instance.items.size
                }

                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    val tvName = holder.itemView.findViewById<TextView>(R.id.tvName)
                    tvName.text = Scanner.instance.items[position].name
                    holder.itemView.setOnClickListener {
                        Scanner.instance.items[position].onClick.invoke()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        doLoad()
    }

    private fun doLoad() {
        reqPermission {
            android.util.Log.d("MainActivity", "Permission granted, loading data")
            Thread {
                runOnUiThread {
                    findViewById<TextView>(R.id.tvTips).text = "Loading..."
                }
                Scanner.instance.loadAll(nesFilesDir)
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Data loaded, items count: ${Scanner.instance.items.size}")
                    recyclerView.adapter?.notifyDataSetChanged()
                    findViewById<TextView>(R.id.tvTips).text = ""
                    // 更新UI显示
                    updateEmptyView()
                }
            }.start()
        }
    }

    private fun updateEmptyView() {
        if (Scanner.instance.items.isEmpty()) {
            recyclerView.visibility = android.view.View.GONE
            tvEmpty.visibility = android.view.View.VISIBLE
        } else {
            recyclerView.visibility = android.view.View.VISIBLE
            tvEmpty.visibility = android.view.View.GONE
        }
    }

    private var lastReqPermissionAt = 0L
    private fun reqPermission(onNext: (() -> Unit)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                onNext.invoke()
            } else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.setData(uri)
                    startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                onNext.invoke()
                return
            }
            if (System.currentTimeMillis() - lastReqPermissionAt < 1000) {
                showToast("权限获取失败")
                return
            }
            lastReqPermissionAt = System.currentTimeMillis()
            // 请求存储权限
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                STORAGE_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_REQUEST_CODE) {
            if (grantResults.isEmpty() || grantResults.size < 2) {
                showToast("权限获取失败 ${grantResults.size}")
            } else {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    doLoad()
                } else {
                    showToast("权限获取失败")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        android.util.Log.d("MainActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        if (requestCode == MANAGE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                android.util.Log.d("MainActivity", "Storage permission granted, loading data")
                doLoad()
            } else {
                showToast("需要存储权限才能扫描游戏文件")
                finish()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    companion object {
        const val MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1002
        const val STORAGE_REQUEST_CODE = 1000

        val nesFilesDir: String by lazy {
            val externalDir = Environment.getExternalStorageDirectory().absolutePath
            // 默认使用外部存储根目录
            externalDir
        }
    }

    private fun showToast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }
}