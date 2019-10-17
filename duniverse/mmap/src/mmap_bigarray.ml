module V1 = struct
  let errno_codes = Unix.[
    EACCES;
    EAGAIN;
    EBADF;
    EINVAL;
    ENFILE;
    ENODEV;
    ENOMEM;
    EPERM;
    EOVERFLOW;

    E2BIG;
    EBUSY;
    ECHILD;
    EDEADLK;
    EDOM;
    EEXIST;
    EFAULT;
    EFBIG;
    EINTR;
    EIO;
    EISDIR;
    EMFILE;
    EMLINK;
    ENAMETOOLONG;
    ENOENT;
    ENOEXEC;
    ENOLCK;
    ENOSPC;
    ENOSYS;
    ENOTDIR;
    ENOTEMPTY;
    ENOTTY;
    ENXIO;
    EPIPE;
    ERANGE;
    EROFS;
    ESPIPE;
    ESRCH;
    EXDEV;
    EWOULDBLOCK;
    EINPROGRESS;
    EALREADY;
    ENOTSOCK;
    EDESTADDRREQ;
    EMSGSIZE;
    EPROTOTYPE;
    ENOPROTOOPT;
    EPROTONOSUPPORT;
    ESOCKTNOSUPPORT;
    EOPNOTSUPP;
    EPFNOSUPPORT;
    EAFNOSUPPORT;
    EADDRINUSE;
    EADDRNOTAVAIL;
    ENETDOWN;
    ENETUNREACH;
    ENETRESET;
    ECONNABORTED;
    ECONNRESET;
    ENOBUFS;
    EISCONN;
    ENOTCONN;
    ESHUTDOWN;
    ETOOMANYREFS;
    ETIMEDOUT;
    ECONNREFUSED;
    EHOSTDOWN;
    EHOSTUNREACH;
    ELOOP;
  ]

  let map_file fd ?pos kind layout shared dims =
    try Bigarray.Genarray.map_file fd ?pos kind layout shared dims
    with Sys_error description as exn ->
      try
        let error_code =
          List.find
            (fun errno -> Unix.error_message errno = description) errno_codes
        in
        raise (Unix.Unix_error (error_code, "Unix.map_file", ""))
      with Not_found ->
        raise exn
end
