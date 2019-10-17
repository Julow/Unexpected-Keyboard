(* $Id$
 * ----------------------------------------------------------------------
 *
 *)

(** Direct access to the package graph and package files *)

type package =
    { package_name : string;
        (** The fully qualified package name, i.e. for subpackages the
	 * names of the containing packages are prepended and the name
	 * components are separated by '.'
	 *)
      package_dir : string;
        (** The directory where to lookup package files *)
      package_meta : string;
        (** The path to the META file *)
      package_defs : Fl_metascanner.pkg_definition list;
        (** The definitions in the META file *)
      package_priv : package_priv;
        (** Private part of the definition *)
    }
    (** The definition of a package *)

and package_priv


val init : string list -> string -> string list -> unit
  (** This function must be called before [Fl_package_base] can be used.
   * The first string corresponds to the [OCAMLPATH] setting, the second
   * string is the location of the standard library. The second is the
   * list of directories with ignored duplicate cmi files.
   *
   * This function is called by {!Findlib.init} and {!Findlib.init_manually},
   * so it is already sufficient to initialize the [Findlib] module.
   *)


(** {1 The package graph} *)

(** The functions in this section operate on a representation of the
 * package graph in memory. The graph is usually only partially available,
 * as only packages are loaded that are queried for.
 *)


exception No_such_package of string * string
  (** First arg is the package name not found, second arg contains additional
   * info for the user. - This is the same exception as in [Findlib].
   *)

exception Package_loop of string
  (** A package is required by itself. The arg is the name of the 
   * package. - This is the same exception as in [Findlib].
   *)

val query : string -> package
  (** Returns the [package] definition for the fully-qualified package name,
   * or raises [No_such_package]. It is allowed to query for subpackages.
   *
   * This function loads package definitions into the graph kept in memory.
   *)

val requires : preds:string list -> string -> string list
  (** Analyzes the direct requirements of the package whose name is passed as
   * second argument under the assumption that the predicates [preds]
   * hold. The function returns the names of the required packages.
   * It is checked whether these packages exist.
   *
   * If there is the "mt" predicate, missing dependencies on "threads"
   * are silently added.
   *
   * The function may raise [No_such_package] or [Package_loop].
   *
   * This function loads package definitions into the graph kept in memory.
   *)

val requires_deeply : preds:string list -> string list -> string list
  (** Analyzes the direct or indirect requirements of the packages whose names
   * are passed as second argument under the assumption that the predicates
   * [preds] hold. The function returns the names of the required packages.
   * It is checked whether these packages exist.
   *
   * If there is the "mt" predicate, missing dependencies on "threads"
   * are silently added.
   *
   * The function may raise [No_such_package] or [Package_loop].
   *
   * This function loads package definitions into the graph kept in memory.
   *)

val package_conflict_report : 
      ?identify_dir:(string -> 'a) -> unit -> unit
  (** Checks whether there are several META files for the same main
   * packages. Complaints are printed to stderr.
   *
   * Only packages in the loaded part of the package graph are checked (i.e.
   * packages for which there was a query).
   *
   * It is recommended to pass the ~identify_dir function whose task
   * it is to return a unique value for every existing directory.
   * For example, 
   *   {[ fun d -> 
   *        let s = Unix.stat d in
   *        (s.Unix.st_dev, s.Unix.st_ino)
   *   ]}
   * could be an implementation for this function. The default is
   * the identity (and not this nice implementation to avoid dependencies
   * on the Unix module).
   *)

val module_conflict_report : ?identify_dir:(string -> 'a) -> string list -> unit
  (** Checks whether there are cmi files for the same modules. The
   * directories passed as first argument are checked. (Note:
   * Neither the '+' nor the '@' notation are recognized.)
   * Complaints about double cmi files are printed to stderr.
   *
   * @param identify_dir See [package_conflict_report].
   *)

val load_base : ?prefix:string -> unit -> unit
  (** Ensures that the complete package graph is loaded into memory.
   * This is a time-consuming operation. Warnings may be printed to
   * stderr.
   *
   * @param prefix Limit to the packages that starts with it. Default: unlimited
   *)

val list_packages : ?prefix:string -> unit -> string list
  (** Ensures that the complete package graph is loaded into memory
   * (like [load_base]), and returns the (unsorted) list of all
   * packages.
   *
   * @param prefix Limit to the packages that starts with it. Default: unlimited
   *)

val package_users : preds:string list -> string list -> string list
  (** Ensures that the complete package graph is loaded into memory
   * (like [load_base]), and determines the packages using one of
   * the packages passed as second argument. The [preds] are assumed
   * for the evaluation of the [requires] directives.
   * The returned list is sorted in ascending order.
   *
   * If there is the "mt" predicate, missing dependencies on "threads"
   * are silently added.
   *
   * Raises [No_such_package] if one of the passed packages cannot
   * be found.
   *)


(** {1 Parsing META files} *)

(** The functions in this section access directly files and directories.
 * The package graph is unknown.
 *)

val packages_in_meta_file : 
      ?directory_required:bool ->
      name:string -> dir:string -> meta_file:string -> unit -> package list
  (** Parses the META file whose name is [meta_file]. In [name], the
   * name of the main package must be passed. [dir] is the
   * directory associated with the package by default (i.e. before
   * it is overriden by the "directory" directive).
   *
   * Returns the package records found in this file. The "directory"
   * directive is already applied.
   *
   * @param directory_required If true, it is checked whether there is a
   * "directory" directive in the main package. If this directive is missing,
   * the function will fail.
   *)

val package_definitions : search_path:string list -> string -> string list
  (** Return all META files defining this package that occur in the 
   * directories mentioned in [search_path]. The package name must be
   * fully-qualified. For simplicity, however, only the name of the main
   * package is taken into account (so it is a good idea to call this
   * function only for main packages).
   *)

