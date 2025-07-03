package com.danp.chat

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel(private val mqttClient: MqttChatClient) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = mqttClient.messages
    val isConnected: StateFlow<Boolean> = mqttClient.isConnected
    val connectionStatus: StateFlow<String> = mqttClient.connectionStatus

    fun connect() {
        mqttClient.connect()
    }

    fun sendMessage(message: String) {
        mqttClient.sendMessage(message)
    }

    fun setUserName(name: String) {
        mqttClient.setUserName(name)
    }

    fun disconnect() {
        mqttClient.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        mqttClient.disconnect()
    }
}
