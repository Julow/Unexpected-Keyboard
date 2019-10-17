(* $Id$
 * ----------------------------------------------------------------------
 *
 *)

(** The primary findlib interface
 *
 * The Findlib module is the primary interface of the findlib library. It
 * contains functions to look up packages, to interpret META
 * files, and to determine the ancestors of packages.
 *
 * This module must be initialized before it can be used: Call either
 * [init] or [init_manually] for this.
 *)

exception No_such_package of string * string
  (** First arg is the package name not found, second arg contains additional
   * info for the user
   *)

exception Package_loop of string
  (** A package is required by itself. The arg is the name of the 
   * package 
   *)


type formal_pred =
    [ `Pred of string     (** Positive occurence of a formal predicate var *)
    | `NegPred of string  (** Negative occurence of a formal predicate var *)
    ]
  (** A formal predicate as it occurs in a package definition *)

val init : 
      ?env_ocamlpath: string ->
      ?env_ocamlfind_destdir: string ->
      ?env_ocamlfind_metadir: string ->
      ?env_ocamlfind_commands: string ->
      ?env_ocamlfind_ignore_dups_in: string ->
      ?env_ocamlfind_ignore_dups_in_list: string list ->
      ?env_camllib: string ->
      ?env_ldconf: string ->
      ?config: string -> 
      ?toolchain: string ->
      unit ->
	unit
  (** Initializes the library from the configuration file and the environment. 
   * By default the
   * function reads the file specified at compile time, but you can also
   * pass a different file name in the [config] argument.
   *   Furthermore, the environment variables OCAMLPATH, OCAMLFIND_DESTDIR, 
   * OCAMLFIND_COMMANDS, OCAMLFIND_IGNORE_DUPS_IN, and CAMLLIB are interpreted.
   * By default, the function takes
   * the values found in the environment, but you can pass different values
   * using the [env_*] arguments. By setting these values to empty strings 
   * they are no longer considered.
   *     The result of the initialization is determined as follows:
   * - The default installation directory is the env variable OCAMLFIND_DESTDIR
   *   (if present and non-empty), and otherwise the variable [destdir] of the
   *   configuration file.
   * - The installation directory for META files is read from the env 
   *   variable OCAMLFIND_METADIR (if present and non-empty), and otherwise
   *   from the variable [metadir] of the configuration file, and otherwise
   *   no such directory is used.
   *   The special value ["none"] turns this feature off.
   * - The search path is the concatenation of the env variable OCAMLPATH
   *   and the variable [path] of the config file
   * - The executables of (ocamlc|ocamlopt|ocamlcp|ocamlmklib|ocamlmktop) are
   *   determined as follows: if the env variable OCAMLFIND_COMMANDS is set
   *   and non-empty, its contents specify the executables. Otherwise, if the
   *   config file variables [ocamlc], [ocamlopt], [ocamlcp], [ocamlmklib] and
   *   [ocamlmktop] are set, their contents specify the executables. Otherwise,
   *   the obvious default values are chosen: ["ocamlc"] for [ocamlc],
   *   ["ocamlopt"] for [ocamlopt], and so on.
   * - The directory of the standard library is the value of the environment
   *   variable CAMLLIB (or OCAMLLIB), or if unset or empty, the value of
   *   the configuration variable [stdlib], or if unset the built-in location
   * - The [ld.conf] file (configuring the dynamic loader) is the value of
   *   the environment variable OCAMLFIND_LDCONF, or if unset or empty, the
   *   value of the configuration variable [ldconf], or if unset the
   *   built-in location.
   * - The ocamlfind tool doesn't emit warnings about double cmi files
   *   for the directories listed in OCAMLFIND_IGNORE_DUPS_IN. Since
   *   findlib-1.8 this variable is interpreted as colon-separated path.
   *   (Before, only one directory could be given.) If the variable is not
   *   set there are no exceptions, and the warnings are always printed.
   *   Note that both the parameters [env_ocamlfind_ignore_dups_in] (a
   *   single directory) and [env_ocamlfind_ignore_dups_in_list] (a list
   *   of directories) override the default.
   *)


val init_manually : 
      ?ocamlc_command: string ->       (* default: "ocamlc"     *)
      ?ocamlopt_command: string ->     (* default: "ocamlopt"   *)
      ?ocamlcp_command: string ->      (* default: "ocamlcp"    *)
      ?ocamloptp_command: string ->    (* default: "ocamloptp"   *)
      ?ocamlmklib_command: string ->   (* default: "ocamlmklib" *)
      ?ocamlmktop_command: string ->   (* default: "ocamlmktop" *)
      ?ocamldep_command: string ->     (* default: "ocamldep"   *)
      ?ocamlbrowser_command: string -> (* default: "ocamlbrowser"   *)
      ?ocamldoc_command: string ->     (* default: "ocamldoc"   *)
      ?ignore_dups_in:string ->        (* default: None *)
      ?ignore_dups_in_list:string list ->  (* default: [] *)
      ?stdlib: string ->               (* default: taken from Findlib_config *)
      ?ldconf: string ->
      ?config: string -> 
      install_dir: string ->
      meta_dir: string ->
      search_path: string list ->
      unit ->
	unit
  (** This is an alternate way to initialize the library directly. 
   * Environment variables and configuration files are ignored. The
   * parameter [config] just sets the file name reported by the
   * [config_file] function below.
   *)


val config_file : unit -> string
  (** The location of the configuration file *)
          
val default_location : unit -> string
  (** Get the default installation directory for packages *)

val meta_directory : unit -> string
  (** Get the META installation directory for packages.
   * Returns [""] if no such directory is configured.
   *)

val search_path : unit -> string list
  (** Get the search path for packages *)

val command : [ `ocamlc | `ocamlopt | `ocamlcp | `ocamloptp | `ocamlmklib 
	      | `ocamlmktop | `ocamldep | `ocamlbrowser | `ocamldoc
	      ] -> 
              string
  (** Get the name/path of the executable *)

val ocaml_stdlib : unit -> string
  (** Get the directory of the standard library *)

val ocaml_ldconf : unit -> string
  (** Get the file name of [ld.conf] *)

val package_directory : string -> string
  (** Get the absolute path of the directory where the given package is
   * stored.
   *
   * Raises [No_such_package] if the package cannot be found.
   *)

val package_meta_file : string -> string
  (** Get the absolute path of the META file of the given package *)

val ignore_dups_in : unit -> string list
  (** If [Some d], duplicate packages below [d] are ignored, and do not
    * produce warnings.  (Only affects the generation of warnings.)
    *
    * Since findlib-1.8 this configuration is a list. Before, it was a
    * [string option].
   *)

val package_property : string list -> string -> string -> string
  (** [package_property predlist pkg propname]:
   * Looks up the property [propname] of package [pkg] under the assumption
   * that the predicates in [predlist] are true.
   *
   * Raises [No_such_package] if the package, and [Not_found] if the property
   * cannot be found.
   *
   * EXAMPLES:
   * - [package_property [] "p" "requires":]
   *   get the value of the [requires] clause of package [p]
   * - [package_property [ "mt"; "byte" ] "p" "archive":]
   *   get the value of the [archive] property of package [p] for multi-
   *   threaded bytecode applications.
   *)

val package_property_2 : string list -> string -> string ->
                         string * formal_pred list
  (** [package_property_2 predlist pkg propname]: This returns two values
      [(v, preds)]. The first one, [v], is computed as in [package_property].
      The other list, [preds], contains the predicates that actually had to
      be set or not set in order to select the particular variable definition.
   *)


val package_ancestors : string list -> string -> string list
  (** [package_ancestors predlist pkg:]
   * Determines the direct ancestors of package [pkg] under the assumption
   * that the predicates in [predlist] are true, i.e. the names of the
   * packages required by [pkg].
   * The returned list is unsorted.
   *
   * Raises [No_such_package] if the package [pkg] or one of its ancestors
   * could not be found.
   *)

val package_deep_ancestors : string list -> string list -> string list
  (** [package_deep_ancestors predlist pkglist:]
   * determines the list of direct or indirect ancestors of the packages
   * named in [pkglist] under the assumption that the predicates in [predlist]
   * are true. 
   *
   * The returned list is topologically sorted: The first element is the
   * deepest ancestor; the last element is one of [pkglist].
   *
   * Raises [No_such_package] if one of the packages in [pkglist] or one of
   * the ancestors cannot be found. Raises [Package_loop] if there is a
   * cyclic dependency.
   *)

val resolve_path : ?base:string -> ?explicit:bool -> string -> string
  (** Resolves findlib notation in filename paths. The notation 
   * [ +name/path ] can be used to refer to the subdirectory [name]
   * of the standard library directory; the continuation [ /path ] is
   * optional. The notation [ \@name/path ] can be used to refer to
   * the directory of the package [name]; the continuation [ /path ]
   * is optional. For these two notations, absolute paths are returned.
   * 
   * @param base When the function is applied on a relative path, the
   *   [base] path is prepended. Otherwise, the path is returned as
   *   it is.
   * @param explicit Changes the meaning of [base] so that only paths
   *   count as relative that include at least one slash.
   *)

val list_packages : ?tab:int -> ?descr:bool -> out_channel -> unit
  (** Prints the list of available packages to the [out_channel].
   *
   * @param tab The tabulator width, by default 20
   * @param descr Whether package descriptions are printed. Default: false
   *)

val list_packages' : ?prefix:string -> unit -> string list
  (** Returns the (unsorted) list of all packages.
   *
   * @param prefix Limit to the packages that starts with it. Default: unlimited
   *)

(** Managing dynamically loaded packages *)

(** This is a registry of packages that are available in-core. This is both
    used for toploops and for plugins.
 *)

type rectype =
  | Record_core  (** The package is part of the executable core *)
  | Record_load  (** The package has been dynamically loaded *)

val record_package : rectype -> string -> unit
  (** Record this package *)

val record_package_predicates : string list -> unit
  (** Record the predicates to be used for package loading. Certain predicates
      are automatically filtered out if inappropriate. A call of
      [record_package_predicates] replaces the set of predicates that was
      installed beforehand.
   *)

val recorded_packages : rectype -> string list
  (** The list of packages recorded with [record_package] *)

val is_recorded_package : string -> bool
  (** Whether there is a recording for this package *)

val type_of_recorded_package : string -> rectype
  (** Returns the type, or raises [Not_found] *)

val recorded_predicates : unit -> string list
  (** The most recent version of the recorded predicate list *)

val reset_recordings : unit -> unit
  (** Removes all [Record_load] packages from the list of recordings.
      This forces that the packages are loaded again.
   *)
