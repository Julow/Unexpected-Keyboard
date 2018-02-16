class%java resources "android.content.res.Resources" =
object
	method get_display_metrics : Android_util.display_metrics = "getDisplayMetrics"
end

class%java context "android.content.Context" =
object
	method get_resources : resources = "getResources"
end
