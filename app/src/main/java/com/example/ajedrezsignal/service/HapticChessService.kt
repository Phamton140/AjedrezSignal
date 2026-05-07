package com.example.ajedrezsignal.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.ajedrezsignal.haptic.HapticFeedbackProvider
import com.example.ajedrezsignal.engine.StockfishBridge

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.Side

class HapticChessService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: FrameLayout
    private lateinit var moveCard: TextView
    private lateinit var gestureDetector: GestureDetector
    private lateinit var hapticProvider: HapticFeedbackProvider
    private lateinit var telephonyManager: TelephonyManager
    private val engine = StockfishBridge()
    private val board = Board()
    private var currentColumn = 0; private var currentRow = 0
    private var originSquare: Square? = null; private var isSelectingOrigin = true
    private var lastBestMove: String? = null
    private var tapCount = 0; private var lastTapTime = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            if (state == TelephonyManager.CALL_STATE_RINGING) stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        hapticProvider = HapticFeedbackProvider(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1, createNotification())
        }

        setupOverlay()
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // El contenedor principal invisible
        overlayView = FrameLayout(this).apply { 
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Creamos la "Tarjeta" visual (TextView con estilo)
        moveCard = TextView(this).apply {
            val shape = GradientDrawable().apply {
                setColor(Color.parseColor("#CC000000")) // Negro traslúcido
                cornerRadius = 30f
                setStroke(3, Color.WHITE)
            }
            background = shape
            setTextColor(Color.WHITE)
            textSize = 32f
            gravity = Gravity.CENTER
            setPadding(40, 20, 40, 20)
            visibility = View.GONE
        }

        val cardParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        overlayView.addView(moveCard, cardParams)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { screenBrightness = 0.01f }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                val dx = e2.x - (e1?.x ?: 0f); val dy = e2.y - (e1?.y ?: 0f)
                if (Math.abs(dx) > Math.abs(dy)) {
                    currentColumn = if (dx > 0) (currentColumn + 1) % 8 else (if (currentColumn == 0) 7 else currentColumn - 1)
                    hapticProvider.vibrateColumn(currentColumn)
                } else {
                    currentRow = if (dy < 0) (currentRow + 1) % 8 else (if (currentRow == 0) 7 else currentRow - 1)
                    hapticProvider.vibrateRow(currentRow)
                }
                return true
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val colChar = ('A' + currentColumn).toChar()
                val rowNum = currentRow + 1
                val sq = Square.fromValue("$colChar$rowNum")

                if (isSelectingOrigin) {
                    originSquare = sq
                    hapticProvider.confirmTone()
                    isSelectingOrigin = false
                } else {
                    originSquare?.let { from ->
                        val m = Move(from, sq)
                        if (board.isMoveLegal(m, true)) {
                            executeMove(m)
                            isSelectingOrigin = true
                            originSquare = null
                        } else {
                            hapticProvider.illegalMoveError()
                        }
                    }
                }
                return true
            }
            override fun onLongPress(e: MotionEvent) { stopSelf() }
        })

        overlayView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val now = System.currentTimeMillis()
                if (now - lastTapTime < 300) tapCount++ else tapCount = 1
                lastTapTime = now
                if (tapCount == 3) {
                    lastBestMove?.let { showMoveVisual(it) }
                    tapCount = 0
                }
            }
            gestureDetector.onTouchEvent(event); true
        }
        windowManager.addView(overlayView, params)
    }

    private fun showMoveVisual(moveStr: String) {
        if (moveStr.length < 4) return
        
        // Formateamos para que sea legible, ej: "e2 -> e4"
        val displayMove = "${moveStr.substring(0, 2).uppercase()} \u279E ${moveStr.substring(2, 4).uppercase()}"
        
        mainHandler.post {
            moveCard.text = displayMove
            moveCard.visibility = View.VISIBLE
            
            // Vibración de aviso
            hapticProvider.confirmTone()
            
            // Ocultar tras 2 segundos
            mainHandler.postDelayed({
                moveCard.visibility = View.GONE
            }, 2000)
        }
    }

    private fun executeMove(move: Move) {
        board.doMove(move)
        val fen = board.fen
        hapticProvider.heartbeat()

        Thread {
            val bestStr = engine.getBestMove(fen)
            lastBestMove = bestStr
            
            // Mostramos la jugada visualmente en vez de la secuencia larga de vibraciones
            showMoveVisual(bestStr)
        }.start()
    }

    private fun createNotification(): Notification {
        val channelId = "ChessHaptic"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, "Service", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(chan)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Motor Háptico Activo")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        }
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
        hapticProvider.illegalMoveError()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}