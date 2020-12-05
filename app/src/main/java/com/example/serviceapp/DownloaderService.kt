package com.example.serviceapp

import android.app.DownloadManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


private const val MSG_REGISTER = 2
private const val MSG_UNREGISTER = 3
private const val MSG_RETVAL = 4

class DownloaderService : Service() {
    val messenger = Messenger(IncomingHandler())
    val clients = mutableListOf<Messenger>()


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val job = GlobalScope.launch(Dispatchers.IO) {
            val urlString = intent!!.getStringExtra(Intent.EXTRA_TEXT)
            val path = downloadImg(urlString!!)
            val broadcastIntent = Intent().apply {
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                putExtra(Intent.EXTRA_TEXT, path)
                action = "picDownloaded"
            }
            sendBroadcast(broadcastIntent)

            Log.d("receiverCUSTOM", "sent")
        }
        stopSelf()
        return START_STICKY
    }



    inner class IncomingHandler() : Handler() {

        override fun handleMessage(msg: Message) {
            val job = GlobalScope.launch(Dispatchers.IO) {
                when (msg.what) {
                    MSG_REGISTER -> {
                        clients.add(msg.replyTo)

                        val urlString = msg.data.getString("urlText")
                        val path = downloadImg(urlString!!)

                        for (client in clients) {
                            try {
                                val retBundle = Bundle()
                                retBundle.putString("address", path)
                                val retMsg = Message.obtain(null, MSG_RETVAL).apply {
                                    data = retBundle
                                }
                                client.send(retMsg)
                            } catch (e: RemoteException) {
                                clients.remove(client)
                            }
                        }
                    }
                    MSG_UNREGISTER -> {
                        clients.remove(msg.replyTo)
                    }
                    else -> super.handleMessage(msg)
                }
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return messenger.binder
    }

    private fun downloadImg(address: String): String {
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path, "pic.jpg")
        val uri = Uri.parse(address)
        val path = file.absolutePath
        Toast.makeText(this, "path is $path", Toast.LENGTH_SHORT).show()

        val request = DownloadManager.Request(uri).setDestinationUri(Uri.fromFile(file))
        dm.enqueue(request)

        return path
    }
}
