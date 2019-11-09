open Android_api.Os

let _handler = lazy (Handler.create ())

let timeout f msec =
	let callback = Jrunnable.create f in
	ignore (Handler.post_delayed (Lazy.force _handler) callback msec)
