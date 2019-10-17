<refentry>

<refmeta>
  <refentrytitle>META</refentrytitle>
  <manvolnum>5</manvolnum>
  <refmiscinfo>The findlib package manager for OCaml</refmiscinfo>
</refmeta>

<refnamediv id="META">
  <refname>META</refname>
  <refpurpose>[File that specifies metainformation of OCaml packages]</refpurpose>
</refnamediv>


<refsynopsisdiv>
<title>GRAMMAR</title>
<synopsis>
         metafile ::= entry*
            entry ::= assignment | addition | subpackage
       subpackage ::= "package" pkgname '(' metafile ')'
       assignment ::= variable_name [ formal_predicates ] '='  value
         addition ::= variable_name [ formal_predicates ] '+=' value
formal_predicates ::= '(' formal_predicate { ',' formal_predicate } ')'
    variable_name ::= name
 formal_predicate ::= name | '-' name
             name ::= [ 'A'-'Z' 'a'-'z' '0'-'9' '_' '.' ]+
          pkgname ::= '"' (character but not '.')* '"'
            value ::= '"' character* '"'
</synopsis>
</refsynopsisdiv>

<refsect1>
<title>DESCRIPTION</title>
<para>
If a package directory contains a file with the fixed name "META" it
is interpreted as described here. The file is a sequence of entries
following the given grammar; every entry defines a variable under a
certain condition given by the list of formal predicates, or it
introduces a subpackage.
</para>

<para>
There is a list of predefined variables and a list of standard
predicates. These variables define: required packages, description, version
information, directories, archive files, and linker options. The
predicates denote circumstances of the application of the variables:
whether the bytecode or the native compiler is used, if there is a
toploop compiled in, details of multi-threading execution, details of
profiling. 
</para>
</refsect1>

<refsect1>
<title>DETAILS OF THE FILE FORMAT</title>
<para>
The file consists of a sequence of entries which must be formed as the
grammar prescribes. The lexical tokens are names, values, and
interpunctuation like '(', ',' and so on. Note that linefeeds do not
play a special role, i.e. an entry definition may be given in more than
one line, or several definitions may occur on a single line. There may
be comments which begin with '#' and run until the end of the line.
</para>

<para>
Names are sequences of the characters A-Z, a-z, 0-9, or _. Names
containing capital letters and names beginning with digits are
allowed but not recommended.
</para>

<para>
Values are enclosed between double quotes. Values may contain any
character. The characters " and \ must be preceded by backslashes. 
</para>

<para>
Package names must not contain the '.' character because it is used
as delimiter of compound names.
</para>

</refsect1>

<refsect1>
<title>MAIN PACKAGES AND SUBPACKAGES</title>
<para>
The outermost variable assignments and additions belong to the main
package. The name of the main package is not defined within META;
it is either the name of the directory containing META or the suffix
of the META file (if the name of the META file is formed like
META.name).</para>

<para>The keyword <literal>package</literal> starts the definition
of a subpackage. There must not be two such definitions with the
same name. Within the parantheses, the variable assignments and
additions refer to the subpackage. It is allowed that a subpackage
contains further subpackages.</para>

<para>The package name following <literal>package</literal>
is the local name relative to the main package, i.e. the
name of the main package is not mentioned. At all other places,
however, the subpackage must be prefixed by the name of the
containing package, separated by a '.'.</para>

<para>Subpackages are independent of the containing package, except
that the subpackage points to the same installation directory as
the containing package (i.e. the location of the installation directory
is inherited from the containing package).</para>
</refsect1>


<refsect1>
<title>SEMANTICS OF VARIABLE DEFINITIONS</title>

<para>
In order to determine the value of a variable, first all assignments
are inspected, and the most specific assignment is taken (if there is
none, the empty string will be taken as value). In a second step,
all additions are gone through one after the other in the order
they occur in the file, and the values of all matching additions are
appended to the current value. In the following, it is further
clarified which assignment is the most specific, which additions
actually match, and how the details of the value addition look like.</para>

<para> The most specific assignment is selected upon a set of actual
predicates, i.e. the set of predicates that are assumed to be true.
The predicates occuring in the definitions of assignments and
additions are called formal predicates. They may be positive or
negative; the latter are prepended by a '-' sign. In order to
determine the value after the evaluation of the assignments, the
following rules apply: </para>

<itemizedlist mark="bullet" spacing="compact"> 
<listitem> 
<para> An assignment can only be used if all positive formal
predicates are included in the set of actual predicates, and if all
negative formal predicates are not included in the set of actual
predicates.  Such an assignment is called
<emphasis>applicable</emphasis>. If there is no such assignment, the
variable will have no value.  
</para> 
</listitem>

<listitem>
<para>
If there is more than one applicable assignment, the definition with
the biggest number of formal predicates is selected.
</para>
</listitem>

<listitem>
<para>
If there is still more than one applicable assignment, both applicable 
and with a maximum number of formal predicates, the definition that is defined
first is selected.
</para>
</listitem>
</itemizedlist>

<para>An addition is matching when all positive formal predicates are
included in the set of actual predicates, and all negative formal
predicates are not included.</para>

<para>The value of an addition is appended to the current value with
implicit white space as separator.</para>

</refsect1>

<refsect1>
<title>VARIABLES</title>

<para>
There is a set of variables with predefined meaning:
</para>

<itemizedlist mark="bullet" spacing="compact"> <listitem> <para> The
variable "directory" redefines the location of the package
directory. Normally, the META file is the first file read in the
package directory, and before any other file is read, the "directory"
variable is evaluated in order to see if the package directory must be
changed. The value of the "directory" variable is determined with an
empty set of actual predicates. The value must be either: an absolute
path name of the alternate directory, or a path name relative to the
stdlib directory of OCaml (written "+path"), or a normal relative path
name (without special syntax). In the latter case, the interpretation
depends on whether it is contained in a main or sub package, and
whether the standard repository layout or the alternate layout is in
effect (see <link linkend="site-lib">site-lib</link> for these terms).
For a main package in standard layout the base directory is the
directory physically containing the META file, and the relative path
is interpreted for this base directory. For a main package in
alternate layout the base directory is the directory physically
containing the META.pkg files. The base directory for subpackages is
the package directory of the containing package. (In the case
that a subpackage definition does not have a "directory" setting,
the subpackage simply inherits the package directory of the containing
package. By writing a "directory" directive one can change this
location again.)
</para> </listitem>

<listitem> 
<para> 
The variable "requires" specifies the list of required packages. The
names of the packages must be separated by white space and/or commas.
The names must be fully qualified (i.e. when they refer to a subpackage,
the names of all containing packages must be prepended, separated by
'.').
</para>
</listitem>

<listitem> 
<para> 
The variable "description" may include a short description of the
package (displayed by <literal>ocamlfind list</literal>).
</para>
</listitem>

<listitem> 
<para> 
The variable "version" specifies the version string.
</para>
</listitem>

<listitem> 
<para> 
The variable "archive" specifies the list of archive files. These
files should be given either as (1) plain names without any directory
information; they are only searched in the package directory.
(2) Or they have the form "+path" in which case the files are looked up
relative to the standard library. (3) Or they have the form "@name/file"
in which case the files are looked up in the package directory
of another package. (4) Or they are given as absolute paths.
</para>

<para>The
names of the files must be separated by white space and/or commas.
In the preprocessor stage, the archive files are passed as extensions
to the preprocessor (camlp4) call. In the linker stage (-linkpkg), the archive
files are linked. In the compiler stage, the archive files are ignored.
</para>

<para>
Note that "archive" should only be used for archive files that are
intended to be included in executables or loaded into toploops. For
modules loaded at runtime there is the separate variable "plugin".
</listitem>

<listitem> 
<para> 
The variable "plugin" specifies the plugin archives of the package.
These can be dynamically loaded with the <literal>Fl_dynload</literal>
module. The plugin archives can have ".cmo", ".cma", or ".cmxs" suffix.
</para>
</listitem>

<listitem> 
<para> 
The variable "linkopts" specifies additional linker options.
</para>
</listitem>

<listitem>
<para>
The variable "error" can be used to signal error conditions. When
this variable is applicable, the ocaml compilers are stopped, and
an error message is printed. The message is the value of the variable.
</para>
</listitem>

<listitem>
<para>
The variable "warning" can be used to signal warnings. When
this variable is applicable, the warning is printed, but the
compilation continues. The message is the value of the variable.
</para>
</listitem>

<listitem> 
<para> 
The variable "exists_if" can be used to disable subpackages. The
value of "exists_if" is a file; the subpackage is hidden if this
file does not exist. You can also enumerate several files, and the
subpackage is hidden if none of the files exist.
</para>
</listitem>

<listitem>
<para>
The variable "ppx" is a command that is added to the compiler invocation
via the -ppx option (available since OCaml-4.01). If the command is
relative to the current directory (e.g. ./cmd), the command is expected
in the package directory. The special forms as defined for "archive"
are also available (e.g. @otherpkg/cmd). Additional arguments can be
specified on the ocamlfind command line with the -ppxopt option
or the "ppxopt" variable.
</para>
</listitem>

<listitem>
<para>
The variable "ppxopt" is a set of options that are added to the ppx
rewriter invocation. The contents of the variable consists of one or
several whitespace-separated parts. Every part consists of several
comma-separated subparts; the first subpart indicates the package
that contains the ppx rewriter invocation, the rest contain the options
to be appended. If the option is a path relative to the current directory
(e.g. ./foo.cma), the path is expanded relative to the package directory.
The special forms as defined for "archive" are also available
(e.g. @otherpkg/foo.cma).
</para>
</listitem>

</itemizedlist>

<para>
It is possible to define additional variables but there is currently
no software interpreting them.
</para>
</refsect1>


<refsect1>
<title>PREDICATES</title>

<para>
There is a list of standard predicates:
</para>

<itemizedlist mark="bullet" spacing="compact">
<listitem>
<para>
The "byte" predicate means that the bytecode compiler is used.
</para>
</listitem>

<listitem>
<para>
The "native" predicate means that the native compiler is used.
</para>
</listitem>

<listitem>
<para>
The "toploop" predicate means that the toploop is available in the
linked program. It is only set when the toploop is running, not when
the toploop is generated.
</para>
</listitem>

<listitem>
<para>
The "create_toploop" predicate means that a toploop is created (using
ocamlmktop).
</para>
</listitem>

<listitem>
<para>
The "mt" predicate means that the program is multi-threaded.
</para>
</listitem>

<listitem>
<para>
The "mt_posix" predicate means that in the case "mt" is set, too, the
POSIX libraries are used to implement threads.
</para>
</listitem>

<listitem>
<para>
The "mt_vm" predicate means that in the case "mt" is set, too, the
VM-based libraries are used to implement threads.
</para>
</listitem>

<listitem>
<para>
The "gprof" predicate means that in the case "native" is set, too, the
program is compiled for profiling
</para>
</listitem>

<listitem>
<para>
The "autolink" predicate means that ocamlc can/will perform automatic linking.
</para>
</listitem>

<listitem>
<para>
The "preprocessor" predicate means that the META variables are scanned for
preprocessor options.</para>
</listitem>

<listitem>
<para>
The "syntax" predicate means that the -syntax option is present on the
command line.</para>
</listitem>

<listitem>
<para>
Legacy: The "plugin" predicate could be used in some versions of findlib
to select cmxs archives instead of cmxa archives. This use is still possible
but discouraged.
</para>
</listitem>

</itemizedlist>

<para>In addition to these predicates, there are package predicates
for every package that is finally selected. Of course, this kind of
predicate must not be used to select "directory" and "requires"
variables, but for the other variables they are perfectly valid.
The package predicates have the form "pkg_" plus the name of the
package (fully qualified).</para>


</refsect1>

</refentry>
