#include <jni.h>
#include <caml/callback.h>

void Java_juloo_ocaml_OCaml_startup(JNIEnv *env, jobject *cls)
{
	char *argv[] = { "", NULL };
	caml_startup(argv);
}
