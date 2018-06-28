type callbacks = <
	onTouchEvent : Android_view.Motion_event.t -> bool;
	onMeasure : int -> int -> unit;
	onDraw : Android_graphics.Canvas.t -> unit;
	onDetachedFromWindow : unit
>

let create (context : _ Android_content.Context.t')
		(f : Android_view.View.t -> callbacks) : Android_view.View.t =
	let cls = Jclass.find_class "juloo/keyboard2/CustomView" in
	let obj_field = Jclass.get_field cls "callbacks" "Ljuloo/javacaml/Value;" in
	let constr = Jclass.get_constructor cls "(Landroid/content/Context;)V" in
	Jcall.push_object context;
	let obj = Jcall.new_ cls constr in
	let callbacks = f obj in
	Jcall.write_field_value obj obj_field callbacks;
	obj
