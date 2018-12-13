package com.mugi111.nearbyconnectionsdatatransfer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.Toast
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.UUID

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var mConnectionClient: ConnectionsClient

    private var opponentEndpointId: String? = null
    private var opponentName: String? = null

    private val strategy = Strategy.P2P_STAR
    private val codeName = UUID.randomUUID().toString()

    private var endpointList: MutableList<String> = mutableListOf()
    var listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, endpointList)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        codename.text = codeName
        status.text = "disconnected"
        mConnectionClient = Nearby.getConnectionsClient(this)
        endpontId_List.adapter = listAdapter

        setContentView(R.layout.activity_main)

        startAdverting()

        opponent_find.setOnClickListener {
            findOpponent()
        }
    }

    override fun onClick(v: View) {
        when(v.id) {
            opponent_find.id -> {
                findOpponent()
            }
            disconnect_button.id -> {
                disconnect()
            }
            send_message.id -> {
                sendMessage()
            }
        }
    }

    private fun disconnect() {
        mConnectionClient.disconnectFromEndpoint(opponentEndpointId.toString())
        status.text = "disconnected"
        opponent_name.text = ""
        opponent_find.isEnabled = true
    }

    private val mPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            Toast.makeText(applicationContext, String(p1.asBytes()!!), Toast.LENGTH_SHORT).show()
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {  }
    }

    private val mEndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(p0: String, p1: DiscoveredEndpointInfo) {
            mConnectionClient.requestConnection(codeName, p0, mConnectionLifecycleCallback)
        }

        override fun onEndpointLost(p0: String) {  }
    }

    private val mConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
            mConnectionClient.acceptConnection(p0, mPayloadCallback)
            opponentName = p1.endpointName
        }

        override fun onDisconnected(p0: String) {
            opponentEndpointId = null
            opponent_name.text = null
            opponent_find.isEnabled = true
        }

        override fun onConnectionResult(p0: String, p1: ConnectionResolution) {
            when (p1.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    mConnectionClient.stopDiscovery()

                    opponentEndpointId = p0
                    status.text = "connected"
                    setOpponentName(opponentName.toString())
                    endpointList.add(p0)
                    listAdapter.notifyDataSetChanged()
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    opponentEndpointId = null
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    opponentEndpointId = null
                }
            }
        }
    }

    private fun findOpponent () {
        mConnectionClient.stopAdvertising()
        startDiscovery()
        opponent_name.text = "Searching"
        opponent_find.isEnabled = false
    }

    private fun startDiscovery() {
        mConnectionClient.startDiscovery(
            packageName, mEndpointDiscoveryCallback, DiscoveryOptions(strategy)).addOnSuccessListener {  }.addOnFailureListener {  }
    }

    private fun startAdverting() {
        mConnectionClient.startAdvertising(
            codeName, packageName, mConnectionLifecycleCallback, AdvertisingOptions(strategy)).addOnSuccessListener {  }.addOnFailureListener {  }
    }

    private fun setOpponentName(opponentName: String) {
        opponent_name.text = opponentName
    }

    private fun sendMessage() {
        mConnectionClient.sendPayload(opponentEndpointId.toString(), Payload.fromBytes("aaa".toByteArray()))
    }

}
