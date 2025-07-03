package com.danp.chat

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.util.*
import javax.net.ssl.SSLSocketFactory

class MqttChatClient(private val context: Context) {
    private var mqttClient: MqttAsyncClient? = null
    private val serverUri = "ssl://bf24a762cf4a4e469aa98b61aae2328d.s1.eu.hivemq.cloud:8883"

    private val clientId = "AndroidChatClient_${UUID.randomUUID()}"
    private val chatTopic = "chat/messages"

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Desconectado")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private var userName: String = "Usuario_${Random().nextInt(1000)}"

    private var mqttUsername: String = ""
    private var mqttPassword: CharArray = charArrayOf()

    fun setUserName(name: String) {
        userName = name
    }

    fun setCredentials(username: String, password: String) {
        mqttUsername = username
        mqttPassword = password.toCharArray()
    }

    fun connect() {
        try {
            _connectionStatus.value = "Conectando..."

            mqttClient = MqttAsyncClient(serverUri, clientId, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 20
                userName = mqttUsername
                password = mqttPassword
                socketFactory = SSLSocketFactory.getDefault()
            }

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    _isConnected.value = false
                    _connectionStatus.value = "Conexión perdida"
                    Log.e("MQTT", "Conexión perdida", cause)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.let {
                        try {
                            val jsonMessage = JSONObject(String(it.payload))
                            val chatMessage = ChatMessage(
                                id = jsonMessage.getString("id"),
                                sender = jsonMessage.getString("sender"),
                                message = jsonMessage.getString("message"),
                                timestamp = jsonMessage.getLong("timestamp"),
                                isOwnMessage = jsonMessage.getString("sender") == userName
                            )
                            _messages.value = _messages.value + chatMessage
                        } catch (e: Exception) {
                            Log.e("MQTT", "Error procesando mensaje", e)
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MQTT", "Mensaje entregado")
                }
            })

            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    _isConnected.value = true
                    _connectionStatus.value = "Conectado"
                    subscribeToTopic()
                    Log.d("MQTT", "Conectado exitosamente")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    _isConnected.value = false
                    _connectionStatus.value = "Error de conexión"
                    Log.e("MQTT", "Error de conexión", exception)
                }
            })

        } catch (e: Exception) {
            _isConnected.value = false
            _connectionStatus.value = "Error de conexión"
            Log.e("MQTT", "Error conectando", e)
        }
    }

    private fun subscribeToTopic() {
        try {
            mqttClient?.subscribe(chatTopic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Suscrito al topic: $chatTopic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Error suscribiéndose al topic", exception)
                }
            })
        } catch (e: Exception) {
            Log.e("MQTT", "Error suscribiéndose", e)
        }
    }

    fun sendMessage(messageText: String) {
        if (!_isConnected.value) return

        try {
            val messageJson = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("sender", userName)
                put("message", messageText)
                put("timestamp", System.currentTimeMillis())
            }

            val message = MqttMessage(messageJson.toString().toByteArray())
            message.qos = 0

            mqttClient?.publish(chatTopic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Mensaje enviado")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Error enviando mensaje", exception)
                }
            })
        } catch (e: Exception) {
            Log.e("MQTT", "Error enviando mensaje", e)
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    _isConnected.value = false
                    _connectionStatus.value = "Desconectado"
                    Log.d("MQTT", "Desconectado")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Error desconectando", exception)
                }
            })
        } catch (e: Exception) {
            Log.e("MQTT", "Error desconectando", e)
        }
    }
}
