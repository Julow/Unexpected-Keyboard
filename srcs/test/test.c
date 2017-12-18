#include <jni.h>

#include <caml/callback.h>
#include <caml/mlvalues.h>

// Wraps caml_named_value
// Returns NULL and throw test.Test.ValueNotFound if not found
value *named_value(JNIEnv *env, char const *name)
{
	value *v = caml_named_value(name);
	if (v != NULL)
		return v;
	jclass exn = (*env)->FindClass(env, "test/Test/ValueNotFound");
	(*env)->ThrowNew(env, exn, "Cannot find callback");
	return NULL;
}

jint Java_test_Test_add(JNIEnv *env, jobject *cls, jint a, jint b)
{
	static value * add = NULL;
	if (add == NULL)
	{
		add = named_value(env, "add");
		if (add == NULL)
			return 0;
	}
	value result = caml_callback2(*add, Val_int((int)a), Val_int((int)b));
	return Int_val(result);
}

jlong Java_test_Test_time(JNIEnv *env, jobject *cls)
{
	static value * time = NULL;
	if (time == NULL)
	{
		time = named_value(env, "time");
		if (time == NULL)
			return 0;
	}
	value result = caml_callback(*time, Val_unit);
	return (jlong)Int64_val(result);
}
