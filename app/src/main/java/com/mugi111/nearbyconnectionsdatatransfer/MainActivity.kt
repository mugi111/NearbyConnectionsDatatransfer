package com.mugi111.nearbyconnectionsdatatransfer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.UUID

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var mConnectionClient: ConnectionsClient

    private var opponentEndpointId: String? = null
    private lateinit var opponentName: String

    private val strategy = Strategy.P2P_STAR
    private val codeName = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        codename.text = codeName
        status.text = "disconnected"
        mConnectionClient = Nearby.getConnectionsClient(this)

        opponent_find.setOnClickListener(this)
    }


    override fun onClick(p0: View?) {
        findOpponent()
    }


    private val mPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {  }

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
            opponent_name.text = "disconnected"
        }

        override fun onConnectionResult(p0: String, p1: ConnectionResolution) {
            when (p1.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    mConnectionClient.stopDiscovery()
                    mConnectionClient.stopAdvertising()

                    opponentEndpointId = p0
                    status.text = "connected"
                    setOpponentName(opponentName)
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
        startAdverting()
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

}
