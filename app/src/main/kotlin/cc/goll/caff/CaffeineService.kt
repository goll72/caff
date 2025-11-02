package cc.goll.caff


import android.app.Service

import android.content.Intent

import android.os.IBinder
import android.os.Binder

import android.os.PowerManager
import android.os.CountDownTimer


private lateinit var WAKE_LOCK: PowerManager.WakeLock;

// Amount of milliseconds in a second
private const val S_TO_MS: Long = 1000


// This service must be explicitly started using @fun startService,
// bound to using @fun bindService and then unbound using @fun unbindService.
//
// It will stop itself automatically when no one is
// bound to it and it isn't holding any wake locks.
class CaffeineService : Service() {
    private val binder = CaffeineBinder()

    // Used to keep track of whether the service has been started or not
    private var lastStartId: Int? = null


    inner class CaffeineBinder : Binder() {
        fun getService(): CaffeineService = this@CaffeineService
    }


    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (!WAKE_LOCK.isHeld) {
            // If the value is null, we weren't started, which shouldn't happen.
            // Either way, we just won't be stopped, so everything is fine.
            stopSelfResult(lastStartId ?: 0)
            lastStartId = null
        }

        return false
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Create a wake lock. This is done once for each execution of the application.
        if (!::WAKE_LOCK.isInitialized) {
            @Suppress("DEPRECATION")
            WAKE_LOCK = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "cc.goll.caff:caffeine"
            )

            WAKE_LOCK.setReferenceCounted(false)
        }

        // NOTE: we don't care about earlier startService calls that may not have finished their lifecycle
        lastStartId = startId
        
        return START_REDELIVER_INTENT
    }


    // How much time has elapsed since the wake lock was acquired
    var elapsed: Int = 0
        private set

    // For how long the current wake lock was acquired originally:
    // @val null if there is no wake lock being held;
    // @val Int.MAX_VALUE if the wake lock was acquired indefinitely
    var acquiredFor: Int? = null
        private set


    private var timer: CountDownTimer? = null


    // Acquire a wake lock for @param duration seconds.
    // 
    // If @param duration is @val Int.MAX_VALUE, acquires a wake
    // lock indefinitely (until @fun releaseWakeLock is called)
    fun acquireWakeLockFor(duration: Int): Boolean {
        if (WAKE_LOCK.isHeld)
            return false

        // Add a grace period of 1s so the user can read the duration shown initially
        var grace = 0

        elapsed = 0
        acquiredFor = duration
        
        WAKE_LOCK.acquire()

        timer = when (duration) {
            Int.MAX_VALUE -> null
            else -> object : CountDownTimer(duration * S_TO_MS, 1 * S_TO_MS) {
                override fun onTick(remaining: Long) {                    
                    if (grace > 0)
                        elapsed++

                    grace = 1
                }

                override fun onFinish() {
                    releaseWakeLock(true)
                }
            }.start()
        }

        return true
    }

    private fun releaseWakeLock(shouldStop: Boolean) {
        elapsed = 0
        acquiredFor = null

        timer?.cancel()

        WAKE_LOCK.release()

        if (shouldStop) {
            stopSelfResult(lastStartId ?: 0)
            lastStartId = null
        }
    }

    fun releaseWakeLock() {
        // If @class CaffeineTileService has asked to release the wake lock,
        // it will probably ask us to acquire a new wake lock soon, so keep
        // running (if that isn't the case, we will be unbound either way)
        releaseWakeLock(false)
    }
}
