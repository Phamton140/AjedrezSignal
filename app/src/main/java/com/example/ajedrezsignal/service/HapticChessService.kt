package com.example.ajedrezsignal.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.example.ajedrezsignal.haptic.HapticFeedbackProvider
import com.example.ajedrezsignal.engine.StockfishBridge
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Move
import com.github.bhlangonijr.chesslib.Square

class HapticChessService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: FrameLayout
    private lateinit var gestureDetector: GestureDetector
    private lateinit var hapticProvider: HapticFeedbackProvider
    private lateinit var telephonyManager: TelephonyManager
    private val engine = StockfishBridge()
    private val board = Board()
    private var currentColumn = 0; private var currentRow = 0
    private var originSquare: Square? = null; private var isSelectingOrigin = true
    private var lastBestMove: String? = null
    private var tapCount = 0; private var lastTapTime = 0L

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            if (state == TelephonyManager.CALL_STATE_RINGING) stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        hapticProvider = HapticFeedbackProvider(this)
        startForeground(1, createNotification())
        setupOverlay()
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = FrameLayout(this).apply { setBackgroundColor(Color.TRANSPARENT); alpha = 0.01f }
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
                val sq = Square.fromValue("${('A' + currentColumn)}${currentRow + 1}")
                if (isSelectingOrigin) { originSquare = sq; hapticProvider.confirmTone(); isSelectingOrigin = false }
                else {
                    originSquare?.let { from ->
                        val m = Move(from, sq)
                        if (board.isMoveLegal(m)) { executeMove(m); isSelectingOrigin = true; originSquare = null }
                        else hapticProvider.illegalMoveError()
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
                if (tapCount == 3) { lastBestMove?.let { hapticProvider.vibrateMove(it) }; tapCount = 0 }
            }
            gestureDetector.onTouchEvent(event); true
        }
        windowManager.addView(overlayView, params)
    }

    private fun executeMove(move: Move) {
        board.doMove(move); val fen = board.fen; hapticProvider.heartbeat()
        Thread {
            val best = engine.getBestMove(fen); lastBestMove = best
            val temp = board.clone(); temp.doMove(Move(best, board.sideToMove))
            hapticProvider.vibrateMove(best, temp.isKingInCheck)
        }.start()
    }

    private fun createNotification(): Notification {
        val channelId = "ChessHaptic"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, "Service", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(chan)
        }
        return NotificationCompat.Builder(this, channelId).setContentTitle("Activo").setSmallIcon(android.R.drawable.ic_lock_idle_lock).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
        hapticProvider.illegalMoveError()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
