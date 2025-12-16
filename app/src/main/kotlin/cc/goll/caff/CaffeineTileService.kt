package cc.goll.caff


import android.content.Intent
import android.content.ServiceConnection
import android.content.ComponentName

import android.graphics.drawable.Icon

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

import android.os.Build
import android.os.IBinder
import android.os.PowerManager

import java.util.concurrent.TimeUnit
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.TreeSet
import java.util.Timer

import cc.goll.caff.utils.HumanReadableTime


class CaffeineTileService : TileService() {
    private lateinit var caffeine: CaffeineService
    private var bound: Boolean = false

    // If Android is optimizing battery usage for the app,
    // we won't be able to start @class CaffeineService
    private var batteryOptimizations: Boolean = false

    private val durations: TreeSet<Int> by lazy {
        CaffeineDurations.get(this)
    }

    private val conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as CaffeineService.CaffeineBinder

            caffeine = binder.getService()
            bound = true

            caffeine.acquiredFor.let {
                iter.reset(it)
            }

            caffeine.persist = true

            updateTile()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bound = false
        }
    }


    // Poor man's tagged union, using inheritance
    open class Duration private constructor () {
        class Active(val duration: Int) : Duration()
        class Inactive() : Duration()
    }


    // Iterator adapter over caffeine durations
    private val iter = object : Iterator<Duration> {
        private lateinit var inner: MutableIterator<Int>

        override fun hasNext(): Boolean = true

        override fun next(): Duration {
            if (!::inner.isInitialized)
                inner = durations.iterator()
            
            if (inner.hasNext())
                return Duration.Active(inner.next())
                    
            inner = durations.iterator()
            return Duration.Inactive()
        }

        // Resets the iterator
        fun reset() {
            inner = durations.iterator()
        }

        // Resets the iterator, starting from the next item after @val duration
        fun reset(duration: Int?) {
            if (duration != null && durations.contains(duration)) {
                inner = durations.tailSet(duration).iterator()
                inner.next()
            } else {
                reset()
            }
        }
    }


    private fun getRemaining(): HumanReadableTime? {
        val acquired = caffeine.acquiredFor
        val elapsed = caffeine.elapsed

        return when (acquired) {
            null -> null
            Int.MAX_VALUE -> null
            else -> HumanReadableTime(acquired - elapsed)
        }
    }


    private final val tileUpdateScheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    private var tileUpdateFuture: ScheduledFuture<*>? = null
    

    private fun updateTile() {
        if (caffeine.acquiredFor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                qsTile.subtitle = getRemaining()?.toString() ?: getString(R.string.qs_tile_infinite)

            qsTile.state = Tile.STATE_ACTIVE
            qsTile.icon = Icon.createWithResource(this, R.drawable.local_cafe_filled)

            tileUpdateFuture = tileUpdateScheduler.schedule({ updateTile() }, 900, TimeUnit.MILLISECONDS)
        } else {
            iter.reset()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                qsTile.subtitle = getString(R.string.qs_tile_off)

            qsTile.state = Tile.STATE_INACTIVE
            qsTile.icon = Icon.createWithResource(this, R.drawable.local_cafe_outline)
        }

        qsTile.updateTile()
    }


    override fun onTileAdded() {
        super.onTileAdded()

        qsTile.label = getString(R.string.qs_tile_label)

        if (batteryOptimizations)
            qsTile.state = Tile.STATE_UNAVAILABLE

        qsTile.updateTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }


    override fun onStartListening() { 
        super.onStartListening()

        val pm: PowerManager = getSystemService(POWER_SERVICE) as PowerManager

        if (!pm.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)) {
            batteryOptimizations = true
            return
        }

        val intent = Intent(this, CaffeineService::class.java)

        startService(intent)
        bindService(intent, conn, 0)
    }

    override fun onStopListening() {
        super.onStopListening()

        tileUpdateFuture?.cancel(false)

        if (!batteryOptimizations)
            unbindService(conn)

        if (::caffeine.isInitialized)
            caffeine.persist = false
    }


    override fun onClick() {
        super.onClick()

        if (batteryOptimizations || !::caffeine.isInitialized) {
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()

            return
        }

        caffeine.releaseWakeLock()

        iter.next().let {
            if (it is Duration.Active) {
                caffeine.acquireWakeLockFor(it.duration)
            }
        } 

        updateTile()
    }
}
