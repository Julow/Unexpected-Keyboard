open Android_api.Content
open Android_api.Graphics
open Android_api.View

type callbacks = <
	onTouchEvent : Motion_event.t -> bool;
	onMeasure : int -> int -> unit;
	onDraw : Canvas.t -> unit;
	onDetachedFromWindow : unit
>

let create (context : _ Context.t')
		(f : View.t -> callbacks) : View.t =
	let cls = Jclass.find_class "juloo/keyboard2/CustomView" in
	let obj_field = Jclass.get_field cls "callbacks" "Ljuloo/javacaml/Value;" in
	let constr = Jclass.get_constructor cls "(Landroid/content/Context;)V" in
	Jcall.push_object context;
	let obj = Jcall.new_ cls constr in
	let callbacks = f obj in
	Jcall.write_field_value obj obj_field callbacks;
	obj
