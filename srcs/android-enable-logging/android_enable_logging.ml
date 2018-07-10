(** Redirect stdin and stdout to android's logging system *)
external enable : string -> unit = "android_enable_logging"
