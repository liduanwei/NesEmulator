package com.ym.nesemulator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.ym.library.ui.gamegallery.GameDescription
import java.io.File
import java.io.FileFilter

class Scanner private constructor() {
    private object InstanceHolder {
        val instance = Scanner()
    }

    companion object {
        val instance: Scanner
            get() = InstanceHolder.instance
    }

    private var isLoading = false
    fun loadAll(baseDir: String) {
        if (isLoading) return Exception("isLoading...").printStackTrace()
        isLoading = true
        items.clear()

        val dirsToSearch = mutableListOf(baseDir)

        val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath
        if (downloadDir != baseDir) {
            dirsToSearch.add(downloadDir)
        }

        val documentsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS).absolutePath
        if (documentsDir != baseDir && documentsDir != downloadDir) {
            dirsToSearch.add(documentsDir)
        }

        val commonGameDirs = listOf("Games", "ROMs", "NES", "Emulator", "Emulation")
        for (dir in commonGameDirs) {
            dirsToSearch.add("$baseDir/$dir")
        }

        android.util.Log.d("Scanner", "Directories to search: $dirsToSearch")

        for (dir in dirsToSearch) {
            loadNesImpl(dir, 0, items)
        }

        android.util.Log.d("Scanner", "Total NES files found: ${items.size}")
        isLoading = false
    }

    private fun loadNesImpl(filepath: String, depth: Int = 0, items: ArrayList<Nes>) {
        if (depth > 20) return
        val file = File(filepath)
        if (!file.exists()) {
            android.util.Log.d("Scanner", "Directory does not exist: $filepath")
            return
        }

        android.util.Log.d("Scanner", "Scanning directory: $filepath")

        val romFiles = file.listFiles(object : FileFilter {
            override fun accept(f: File): Boolean {
                if (f.isDirectory) {
                    loadNesImpl(f.absolutePath, depth + 1, items)
                    return false
                }
                return f.name.endsWith(".nes") || f.name.endsWith(".NES")
            }
        })?.map {
            Nes(it.name, it.absolutePath) {
                startGame(App.instance, it.path)
            }
        }

        romFiles?.let {
            android.util.Log.d("Scanner", "Found ${it.size} NES files in $filepath")
            items.addAll(it)
        }
    }


    fun startGame(context: Context, nesRomFile: String) {
        val romFile = File(nesRomFile)
        if (!romFile.exists()) {
            Toast.makeText(App.instance, "the file not exists", Toast.LENGTH_SHORT).show()
            return
        }
        // 默认游戏界面
//        EmulatorManager.getInstance().startGame(this, romFile)
        //自定义游戏界面
        //EmulatorManager.getInstance().startGame(activity, MyEmulatorActivity::class.java, romFile)
        val var10001 = Intent(context, MyEmulatorActivity::class.java)
        val var6 = GameDescription(romFile)
        var10001.putExtra("game", var6)
        var10001.putExtra("slot", 0)
        var10001.putExtra("fromGallery", true)
        if (context !is Activity) var10001.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(var10001)
    }

    var items = ArrayList<Nes>()

    data class Nes(val name: String, val path: String, val onClick: (() -> Unit))
}