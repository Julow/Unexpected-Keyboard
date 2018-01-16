module System =
struct

	let system_class = Java.Class.find_class "java/lang/System"

	let currentTimeMillis =
		let m = Java.Class.get_meth_static system_class "currentTimeMillis" "()J" in
		fun () ->
			Java.meth_static system_class m;
			Java.call_int64 ()

end

let () =
	Callback.register "add" (+);
	Callback.register "time" System.currentTimeMillis
