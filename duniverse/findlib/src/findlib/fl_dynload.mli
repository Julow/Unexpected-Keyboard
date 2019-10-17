(* $Id$ *)

(** Utilities for loading dynamically packages *)

val load_packages : ?debug:bool -> string list -> unit
(** Load the given packages and all their dependencies dynamically. Packages
    already loaded or already in-core are not loaded again. The predicates
    are taken from {!Findlib.recorded_predicates}, which are normally the
    predicates from the link-time of the executable.

    In order to initialize this module correctly, you need to link the
    executable in a special way. This is done by including "findlib.dynload"
    in the [ocamlfind] command, e.g.

    {[ ocamlfind ocamlopt -o program -package findlib.dynload -linkpkg m.ml ]}

    It is not sufficient to just link [findlib_dynload.cm(x)a] into the
    executable. The above command adds special initialization code that
    (a) records the predicates and (b) records the packages already present
    in the executable. Also [-linkall] is implicitly added.

    The dynamic package loader works both for bytecode and native code.
    The META files of the packages need to specify the cma or cmxs files
    in the following way:

     - First, the "plugin" variable is checked (instead of "archive"), e.g.
       {[
plugin(byte) = "my_plugin.cma"
plugin(native) = "my_plugin.cmxs"
       ]}
       This is the preferred style.
     - Second, for bytecode only, the normal "archive" variable is
       also accepted if "plugin" is not present. (Because bytecode archives
       can normally be dynamically loaded without special preparation.)
     - Third, for native-code only, the "archive(plugin)" variable
       is also accepted. This is for legacy packages.
 *)
