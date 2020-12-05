package com.example.serviceapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

private const val MSG_REGISTER = 2
private const val MSG_UNREGISTER = 3
private const val MSG_RETVAL = 4

class MainActivity : AppCompatActivity() {
    private var serviceMessenger: Messenger? = null
    private lateinit var tv: TextView
    var isBound = false

    inner class activityHandler: Handler() {
        override fun handleMessage(msg: Message) {
            when(msg.what) {
                MSG_RETVAL -> {
                    tv.text = msg.data.getString("address")
                }
                else -> super.handleMessage(msg)
            }
        }
    }
    private var activityMessenger: Messenger?  = Messenger(activityHandler())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(compName: ComponentName, binder: IBinder) {
            serviceMessenger = Messenger(binder)
            try {
                val retBundle = Bundle()
                retBundle.putString("urlText", "https://smartminds.ru/wp-content/uploads/2019/12/foto-2-kartinka-s-pozhelaniem-prekrasnogo-nastroeniya-na-ves-den.jpg")
                Toast.makeText(this@MainActivity, retBundle.getString("urlText"), Toast.LENGTH_SHORT).show()
                val msgStartDownload = Message.obtain(null, MSG_REGISTER).apply {
                    data = retBundle
                    replyTo = activityMessenger
                }
                serviceMessenger!!.send(msgStartDownload)

            } catch (e: RemoteException) {
                //disconnection logic is in service, no need to worry
            }
        }
        override fun onServiceDisconnected(compName: ComponentName) {
            serviceMessenger = null
        }
    }

    private fun doBindService() {
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        Toast.makeText(this, "BINDED", Toast.LENGTH_SHORT).show()
        isBound = true
    }

    private fun doUnbindService() {
        if (isBound) {
            try {
                val msg = Message.obtain(null, MSG_UNREGISTER)
                msg.replyTo = activityMessenger
                serviceMessenger!!.send(msg)
            } catch (e: RemoteException) {

            }
            unbindService(connection)
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv = textView


        button.setOnClickListener {
            intent = Intent(this, DownloaderService::class.java)
                .putExtra(Intent.EXTRA_TEXT, "https://smartminds.ru/wp-content/uploads/2019/12/foto-2-kartinka-s-pozhelaniem-prekrasnogo-nastroeniya-na-ves-den.jpg")
            //startService(intent)

            doBindService()
        }
        button2.setOnClickListener {
            doUnbindService()
        }
    }
}