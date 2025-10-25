package cc.goll.caff


import android.content.Intent
import android.content.ServiceConnection
import android.content.ComponentName

import android.graphics.drawable.Icon

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.IBinder

import java.util.concurrent.TimeUnit
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.SortedSet
import java.util.Timer


private val CAFFEINE_DURATIONS: SortedSet<Int> =
        sortedSetOf(5 * 60, 10 * 60, 30 * 60, Int.MAX_VALUE)

// Maximum number of retries when waiting for
// @class CaffeineService after a click event
private const val MAX_RETRIES: Int = 4


class CaffeineTileService : TileService() {
    // XXX: handle cases where this isn't set
    private lateinit var caffeine: CaffeineService
    private var bound: Boolean = false

    private val conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as CaffeineService.CaffeineBinder

            caffeine = binder.getService()
            bound = true
            
            val acquired = caffeine.acquiredFor

            if (acquired != null && CAFFEINE_DURATIONS.contains(acquired)) {
                // NOTE: can be improved from O(n) to O(log n) by using
                // @fun headSet and @fun tailSet; for now this will do
                do {
                    val it = iter.next()

                    val found: Boolean = when (it) {
                        is Duration.Active -> it.duration == acquired
                        else -> false
                    }
                } while (!found)
            }

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
    private var iter = object : Iterator<Duration> {
        // I'd rather call this inner but that's a Kotlin keyword
        private var detail = CAFFEINE_DURATIONS.iterator()

        override fun hasNext(): Boolean = true

        override fun next(): Duration {
            if (detail.hasNext())
                return Duration.Active(detail.next())
                    
            detail = CAFFEINE_DURATIONS.iterator()
            return Duration.Inactive()
        }

        fun reset() {
            detail = CAFFEINE_DURATIONS.iterator()
        }
    }
    

    private class HumanReadableTime(t: Int) {
        private val t: Int = t;

        fun get(): Int = t;
        
        override fun toString(): String =
            "%02d:%02d".format(t / 60, t % 60);
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
            qsTile.label = "Caffeine on"
            qsTile.subtitle = getRemaining()?.toString() ?: "infinite"

            qsTile.state = Tile.STATE_ACTIVE
            qsTile.icon = Icon.createWithResource(this, R.drawable.local_cafe_filled)

            tileUpdateFuture = tileUpdateScheduler.schedule({ updateTile() }, 900, TimeUnit.MILLISECONDS)
        } else {
            iter.reset()
                
            qsTile.label = "Caffeine off"
            qsTile.subtitle = String()

            qsTile.state = Tile.STATE_INACTIVE
            qsTile.icon = Icon.createWithResource(this, R.drawable.local_cafe_outline)
        }

        qsTile.updateTile()
    }


    override fun onTileAdded() {
        super.onTileAdded()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }


    override fun onStartListening() { 
        super.onStartListening()
        
        val intent = Intent(this, CaffeineService::class.java)

        startService(intent)
        bindService(intent, conn, 0)
    }

    override fun onStopListening() {
        super.onStopListening()

        tileUpdateFuture?.cancel(false)

        unbindService(conn)
    }


    override fun onClick() {
        for (i in 0..MAX_RETRIES) {
            if (bound)
                break

            Thread.sleep(200)
        } 

        super.onClick()

        // XXX: @val caffeine may not be set even after waiting
        caffeine.releaseWakeLock()

        val it = iter.next()

        when (it) {
            is Duration.Active -> caffeine.acquireWakeLockFor(it.duration)
        }

        updateTile()
    }
}
