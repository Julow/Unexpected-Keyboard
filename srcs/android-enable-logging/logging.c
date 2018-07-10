#include <pthread.h>
#include <unistd.h>

#include <android/log.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>

/** Launch a detached thread that redirect stdout and stderr
	to android's logs
	https://stackoverflow.com/a/42715692/5603862 */

value g_tag;
int g_fd = -1;

static void *do_logging(void *param)
{
	char buff[256];
	ssize_t len;

	while ((len = read(g_fd, buff, sizeof(buff) - 1)) > 0)
	{
		buff[len] = '\0';
		__android_log_write(ANDROID_LOG_DEBUG, String_val(g_tag), buff);
	}
	return NULL;
}

value android_enable_logging(value tag)
{
	pthread_t thrd;
	int fds[2];

	if (g_fd != -1)
		return Val_unit;
	g_tag = tag;
	caml_register_generational_global_root(&g_tag);
	pipe(fds);
	dup2(fds[1], 1);
	dup2(fds[1], 2);
	g_fd = fds[0];
	pthread_create(&thrd, NULL, &do_logging, NULL);
	pthread_detach(thrd);
	return Val_unit;
}
