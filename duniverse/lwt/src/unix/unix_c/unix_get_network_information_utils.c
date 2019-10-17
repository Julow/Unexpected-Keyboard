/* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. */



#include "lwt_config.h"

#if !defined(LWT_ON_WINDOWS)

#include <caml/alloc.h>
#include <caml/memory.h>
#include <caml/mlvalues.h>
#include <caml/socketaddr.h>
#include <stdlib.h>
#include <string.h>

#include "unix_get_network_information_utils.h"

#if defined(NON_R_GETHOSTBYADDR) || defined(NON_R_GETHOSTBYNAME)
char **c_copy_addr_array(char **src, int addr_len)
{
    if (src == NULL) {
        return NULL;
    }
    char **p = src;
    size_t i = 0;
    while (*p) {
        i++;
        p++;
    }
    const size_t ar_len = i;
    p = malloc((ar_len + 1) * sizeof(char *));
    if (p == NULL) {
        return NULL;
    }
    for (i = 0; i < ar_len; ++i) {
        p[i] = malloc(addr_len);
        if (p[i] == NULL) {
            size_t j;
            for (j = 0; j < i; j++) {
                free(p[j]);
            }
            free(p);
            return NULL;
        }
        memcpy(p[i], src[i], addr_len);
    }
    p[ar_len] = NULL;
    return p;
}
#endif

#if !defined(HAVE_NETDB_REENTRANT) || defined(NON_R_GETHOSTBYADDR) || \
    defined(NON_R_GETHOSTBYNAME)
char **c_copy_string_array(char **src)
{
    char **p = src;
    size_t i = 0;
    size_t len;
    if (src == NULL) {
        return NULL;
    }
    while (*p) {
        i++;
        p++;
    }
    len = i;
    p = malloc((len + 1) * sizeof(char *));
    if (p == NULL) {
        return NULL;
    }
    for (i = 0; i < len; ++i) {
        p[i] = strdup(src[i]);
        if (p[i] == NULL) {
            size_t j;
            for (j = 0; j < i; j++) {
                free(p[j]);
            }
            free(p);
            return NULL;
        }
    }
    p[len] = NULL;
    return p;
}

void c_free_string_array(char **src)
{
    if (src) {
        char **p = src;
        while (*p) {
            free(*p);
            ++p;
        }
        free(src);
    }
}

char *s_strdup(const char *s)
{
    return (strdup(s == NULL ? "" : s));
}
#endif

CAMLexport value alloc_inet_addr(struct in_addr *inaddr);
CAMLexport value alloc_inet6_addr(struct in6_addr *inaddr);

static value alloc_one_addr(char const *a)
{
    struct in_addr addr;
    memmove(&addr, a, 4);
    return alloc_inet_addr(&addr);
}

static value alloc_one_addr6(char const *a)
{
    struct in6_addr addr;
    memmove(&addr, a, 16);
    return alloc_inet6_addr(&addr);
}

value alloc_host_entry(struct hostent *entry)
{
    value res;
    value name = Val_unit, aliases = Val_unit;
    value addr_list = Val_unit, adr = Val_unit;

    Begin_roots4(name, aliases, addr_list, adr);
    name = caml_copy_string((char *)(entry->h_name));
    /* PR#4043: protect against buggy implementations of gethostbynamee()
       that return a NULL pointer in h_aliases */
    if (entry->h_aliases)
        aliases = caml_copy_string_array((const char **)entry->h_aliases);
    else
        aliases = Atom(0);
    if (entry->h_length == 16)
        addr_list = caml_alloc_array(alloc_one_addr6,
                                     (const char **)entry->h_addr_list);
    else
        addr_list =
            caml_alloc_array(alloc_one_addr, (const char **)entry->h_addr_list);
    res = caml_alloc_small(4, 0);
    Field(res, 0) = name;
    Field(res, 1) = aliases;
    switch (entry->h_addrtype) {
        case PF_UNIX:
            Field(res, 2) = Val_int(0);
            break;
        case PF_INET:
            Field(res, 2) = Val_int(1);
            break;
        default: /*PF_INET6 */
            Field(res, 2) = Val_int(2);
            break;
    }
    Field(res, 3) = addr_list;
    End_roots();
    return res;
}

#if defined(NON_R_GETHOSTBYADDR) || defined(NON_R_GETHOSTBYNAME)
struct hostent *hostent_dup(struct hostent *orig)
{
    if (orig == NULL) {
        return NULL;
    }
    struct hostent *h = malloc(sizeof *h);
    if (h == NULL) {
        return NULL;
    }
    h->h_name = s_strdup(orig->h_name);
    if (!h->h_name) {
        goto nomem1;
    }
    if (!orig->h_aliases) {
        h->h_aliases = NULL;
    } else {
        h->h_aliases = c_copy_string_array(orig->h_aliases);
        if (!h->h_aliases) {
            goto nomem2;
        }
    }
    if (!orig->h_addr_list) {
        h->h_addr_list = NULL;
    } else {
        h->h_addr_list = c_copy_addr_array(orig->h_addr_list, orig->h_length);
        if (!h->h_addr_list) {
            goto nomem3;
        }
    }
    h->h_addrtype = orig->h_addrtype;
    h->h_length = orig->h_length;
    return h;
nomem3:
    c_free_string_array(h->h_aliases);
nomem2:
    free((char *)h->h_name);
nomem1:
    free(h);
    return NULL;
}

void hostent_free(struct hostent *h)
{
    if (h) {
        c_free_string_array(h->h_addr_list);
        c_free_string_array(h->h_aliases);
        free((char *)h->h_name);
        free(h);
    }
}
#endif

#endif
