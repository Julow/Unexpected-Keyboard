(* $Id$
 * ----------------------------------------------------------------------
 *
 *)

(** Parses META files *)

open Fl_metatoken

type formal_pred =
    [ `Pred of string     (** Positive occurence of a formal predicate var *)
    | `NegPred of string  (** Negative occurence of a formal predicate var *)
    ]

type flavour =
    [ `BaseDef 
    | `Appendix 
    ]
  (** [`BaseDef] refers to META definitions using the "=" operator,
   * and [`Appendix] refers to definitions using the "+=" operator.
   *)

type pkg_definition =
    { def_var : string;              (** The name of the defined variable *)
      def_flav : flavour;            (** The flavour of the definition *)
      def_preds : formal_pred list;  (** The formal predicates of the def *)
      def_value : string;            (** The value assigned to the variable *)
    }
  (** A [pkg_definition] is expressed by the syntax
   *  {[ var(p1,p2,...) = "value" ]} (flavour `BaseDef), 
   *  or the syntax
   *  {[ var(p1,p2,...) += "value" ]} (flavour `Appendix)
   *  in the META file. The list of predicates may be omitted. Predicates
   *  may be negated by using "-", e.g. "-x".
   *)

type pkg_expr =
    { pkg_defs : pkg_definition list;
      pkg_children : (string * pkg_expr) list;
    }
  (** A value of type [pkg_expr] denotes the contents of a META file.
   *  The component [pkg_defs] are the variable definitions.
   *  The component [pkg_children] contains
   *  the definitions of the subpackages.
   *)


val parse : in_channel -> pkg_expr
  (** [parse ch:] 
   * scans and parses the file connected with channel [ch]. The file must
   * have a syntax compatible with the META format. The return value
   * contains the found definitions for the package and all subpackages.
   *
   * [exception Stream.Error of string:] is
   * raised on syntax errors. The string explains the error.
   *)

val parse2 : in_channel -> pkg_expr

val parse2_lexing : Lexing.lexbuf -> pkg_expr
val parse_lexing : Lexing.lexbuf -> pkg_expr


val print_def : out_channel -> pkg_definition -> unit
  (** [print_def ch def]:
    * Outputs the definition to a channel.
   *)

val print : out_channel -> pkg_expr -> unit
  (** [print ch expr]:
    * Outputs the package expression to a channel.
   *)


val lookup : 
    string -> string list -> pkg_definition list -> string
  (** [lookup variable_name predicate_list def]:
   *
   * Returns the value of [variable_name] in [def] under the assumption
   * that the predicates in [predicate_list] hold, but no other predicates.
   *
   * The rules are as follows: In the step (A), only the [`BaseDef]
   * definitions are considered. The first base definition is determined where
   * all predicates are satisfied and that has the longest predicate list.
   * In the step (B) only the [`Appendix] definitions are considered.
   * All definitions are determined where all predicates are satisfied.
   * The final result is the concatenation of the single result of (A)
   * and all results of (B) (in the order they are defined). A space
   * character is inserted between two concatenated strings.
   *
   * When step (A) does not find any matching definition, the exception
   * [Not_found] is raised.
   *)


val lookup_2 : 
    string -> string list -> pkg_definition list -> string * formal_pred list
  (** Like [lookup], but also returns the list of predicates that had to
      be considered to select the particular variable definition.
   *)


val predicate_exists :
    string -> pkg_definition list -> bool
  (** [predicate_exists variable_name def]:

      Whether [variable_name] is explicitly mentioned in [def].
   *)
