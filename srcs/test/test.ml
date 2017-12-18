
module System =
struct

	let system_class = Jni.find_class "java/lang/System"

	let currentTimeMillis =
		let met = Jni.get_static_methodID system_class "currentTimeMillis" "()J" in
		fun () ->
			Jni.call_static_long_method system_class met [||]

end

let () =
	Callback.register "add" (+);
	Callback.register "time" System.currentTimeMillis
