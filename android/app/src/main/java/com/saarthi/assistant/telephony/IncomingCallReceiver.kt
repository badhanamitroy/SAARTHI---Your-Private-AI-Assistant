package com.saarthi.assistant.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

/**
 * BroadcastReceiver for incoming phone call state changes.
 * Listens for android.intent.action.PHONE_STATE.
 * Dispatches state transitions directly to CallCoordinator.
 */
class IncomingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

        @Suppress("DEPRECATION")
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val callState = when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> CallState.RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> CallState.OFFHOOK
            TelephonyManager.EXTRA_STATE_IDLE -> CallState.IDLE
            else -> return
        }

        CallCoordinator.instance?.onCallStateChanged(callState, incomingNumber)
    }
}
