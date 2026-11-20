package com.example.sahakarbharati.features.yatra

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.example.sahakarbharati.MainActivity
import com.example.sahakarbharati.R
import com.example.sahakarbharati.SahakarApp
import com.example.sahakarbharati.data.QueuedFix
import com.example.sahakarbharati.data.FlushAction
import com.example.sahakarbharati.data.flushAction
import com.example.sahakarbharati.data.nextBackoffMs
import com.example.sahakarbharati.data.shouldCaptureFix
import com.example.sahakarbharati.data.trackerFingerprint
import com.example.sahakarbharati.data.repository.PositionSubmit
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground GPS capture for an assigned tracker. Started from the tracking screen;
 * does not request ACCESS_BACKGROUND_LOCATION.
 */
class YatraTrackingService : Service(), LocationListenerCompat {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var yatraId: String = ""
    private var intervalMs: Long = DEFAULT_INTERVAL_MS
    private var lastSentAt: String = "never"
    private var lastError: String = ""
    private var drops = 0
    private var backoffMs = 5_000L
    private var flushJob: Job? = null
    private val lastFixAge = AtomicLong(0)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelfSafe()
                return START_NOT_STICKY
            }
            else -> {
                yatraId = intent?.getStringExtra(EXTRA_YATRA_ID).orEmpty()
                intervalMs = intent?.getLongExtra(EXTRA_INTERVAL_MS, DEFAULT_INTERVAL_MS) ?: DEFAULT_INTERVAL_MS
                startForegroundTracking()
                startLocationUpdates()
                scheduleFlush(immediate = true)
            }
        }
        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        val mocked = if (Build.VERSION.SDK_INT >= 31) location.isMock else @Suppress("DEPRECATION") location.isFromMockProvider
        val ageMs = (System.currentTimeMillis() - location.time).coerceAtLeast(0)
        lastFixAge.set(ageMs)
        if (!shouldCaptureFix(mocked, ageMs, intervalMs, location.accuracy, 100f)) return
        val app = application as SahakarApp
        val token = app.container.session.trackerToken() ?: return
        val fingerprint = trackerFingerprint(token)
        app.container.positionQueue.enqueue(
            QueuedFix(
                yatraId = yatraId,
                clientPositionId = UUID.randomUUID().toString(),
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyM = location.accuracy.toDouble(),
                capturedAt = Instant.ofEpochMilli(location.time).toString(),
                credentialFingerprint = fingerprint,
            ),
        )
        updateNotification()
        scheduleFlush(immediate = true)
    }

    private fun startForegroundTracking() {
        ensureChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startLocationUpdates() {
        val manager = getSystemService(LocationManager::class.java) ?: return
        val request = LocationRequestCompat.Builder(intervalMs)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(intervalMs)
            .build()
        runCatching {
            LocationManagerCompat.requestLocationUpdates(
                manager,
                LocationManager.GPS_PROVIDER,
                request,
                ContextCompat.getMainExecutor(this),
                this,
            )
        }
    }

    private fun scheduleFlush(immediate: Boolean) {
        flushJob?.cancel()
        flushJob = scope.launch {
            if (!immediate) delay(backoffMs)
            flushOnce()
        }
    }

    private suspend fun flushOnce() {
        val app = application as SahakarApp
        val token = app.container.session.trackerToken()
        if (token.isNullOrBlank()) {
            lastError = "Tracker credential missing"
            stopSelfSafe()
            return
        }
        val fingerprint = trackerFingerprint(token)
        val batch = app.container.positionQueue.snapshot()
            .filter { it.yatraId == yatraId && it.credentialFingerprint == fingerprint }
            .take(50)
        if (batch.isEmpty()) {
            updateNotification()
            return
        }
        when (val result = app.container.yatraFieldRepository.postPositionBatch(yatraId, batch)) {
            is PositionSubmit.Accepted -> {
                batch.forEach { app.container.positionQueue.remove(it.clientPositionId) }
                lastSentAt = Instant.now().toString()
                lastError = ""
                backoffMs = 5_000L
            }
            is PositionSubmit.Http -> {
                when (flushAction(result.status)) {
                    FlushAction.ClearCredential -> {
                        app.container.positionQueue.discardFingerprint(fingerprint)
                        app.container.session.clearTrackerToken()
                        lastError = result.message
                        stopSelfSafe()
                        return
                    }
                    FlushAction.StopPermanent -> {
                        lastError = result.message
                        stopSelfSafe()
                        return
                    }
                    FlushAction.Drop -> {
                        batch.forEach { app.container.positionQueue.remove(it.clientPositionId) }
                        drops += batch.size
                        lastError = result.message
                    }
                    FlushAction.Remove -> batch.forEach { app.container.positionQueue.remove(it.clientPositionId) }
                    FlushAction.Keep -> {
                        lastError = result.message
                        backoffMs = nextBackoffMs(backoffMs)
                        scheduleFlush(immediate = false)
                        updateNotification()
                        return
                    }
                }
            }
            PositionSubmit.NoCredential -> {
                lastError = "Tracker credential missing"
                stopSelfSafe()
                return
            }
        }
        updateNotification()
        if (app.container.positionQueue.snapshot().any { it.yatraId == yatraId }) {
            scheduleFlush(immediate = false)
        }
    }

    private fun buildNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val queued = runCatching {
            (application as SahakarApp).container.positionQueue.snapshot().count { it.yatraId == yatraId }
        }.getOrDefault(0)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Yatra tracking")
            .setContentText("Queued $queued · last sent $lastSentAt · ${lastError.ifBlank { "ok" }}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Yatra tracking", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun stopSelfSafe() {
        runCatching {
            val manager = getSystemService(LocationManager::class.java)
            if (manager != null) LocationManagerCompat.removeUpdates(manager, this)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_STOP = "com.example.sahakarbharati.STOP_TRACKING"
        const val EXTRA_YATRA_ID = "yatra_id"
        const val EXTRA_INTERVAL_MS = "interval_ms"
        private const val CHANNEL_ID = "yatra_tracking"
        private const val NOTIFICATION_ID = 42
        private const val DEFAULT_INTERVAL_MS = 15_000L

        fun start(context: Context, yatraId: String, intervalMs: Long = DEFAULT_INTERVAL_MS) {
            val intent = Intent(context, YatraTrackingService::class.java)
                .putExtra(EXTRA_YATRA_ID, yatraId)
                .putExtra(EXTRA_INTERVAL_MS, intervalMs)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, YatraTrackingService::class.java).setAction(ACTION_STOP))
        }
    }
}
