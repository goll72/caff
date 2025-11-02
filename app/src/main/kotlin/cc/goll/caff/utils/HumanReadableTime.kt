package cc.goll.caff.utils


class HumanReadableTime(private val t: Int) {
    fun get(): Int = t;
    
    override fun toString(): String =
        "%02d:%02d".format(t / 60, t % 60);
}
