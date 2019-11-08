class%java system_clock "android.os.SystemClock" =
object
	method [@static] uptime_millis : long = "uptimeMillis"
end

class%java handler "android.os.Handler" =
object
	initializer (create : _)
	method post_delayed : runnable -> long -> bool = "postDelayed"
end
