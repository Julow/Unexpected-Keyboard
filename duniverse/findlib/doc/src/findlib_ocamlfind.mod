<refentry>

<refmeta>
  <refentrytitle>ocamlfind</refentrytitle>
  <manvolnum>1</manvolnum>
  <refmiscinfo>The findlib package manager for OCaml</refmiscinfo>
</refmeta>

<refnamediv id="ocamlfind">
  <refname>ocamlfind</refname>
  <refpurpose>[Command-line interface of the Package manager]</refpurpose>
</refnamediv>


<refsynopsisdiv>
<title>SYNOPSIS</title>
<synopsis>
       <link linkend="ocamlfind.query">ocamlfind query [-help | other options] <replaceable>package_name</replaceable> ...</link>
   or: <link linkend="ocamlfind.ocamlc">ocamlfind ocamlc [-help | other options] <replaceable>file</replaceable> ...</link>
   or: <link linkend="ocamlfind.ocamlcp">ocamlfind ocamlcp [-help | other options] <replaceable>file</replaceable> ...</link>
   or: <link linkend="ocamlfind.ocamlmktop">ocamlfind ocamlmktop [-help | other options] <replaceable>file</replaceable> ...</link>
   or: <link linkend="ocamlfind.ocamlopt">ocamlfind ocamlopt [-help | other options] <replaceable>file</replaceable> ...</link>
   or: <link linkend="ocamlfind.ocamldoc">ocamlfind ocamldoc [-help | other options] <replaceable>file</replaceable> ...</link>
   or: <link linkend="ocamlfind.ocamldep">ocamlfind ocamldep [-help | other options] <replaceable>file</replaceable> ...</link>
   or: <link linkend="ocamlfind.ocamlmklib">ocamlfind ocamlmklib [-help | other options] <replaceable>file</replaceable> ...</link>
   or: <link linkend="ocamlfind.ocamlbrowser">ocamlfind ocamlbrowser [-help | other options]</link>
   or: <link linkend="ocamlfind.install">ocamlfind install [-help | other options] <replaceable>package_name</replaceable> <replaceable>file</replaceable> ...</link>
   or: <link linkend="ocamlfind.remove">ocamlfind remove [-help | other options] <replaceable>package_name</replaceable></link>
   or: <link linkend="ocamlfind.list">ocamlfind lint <replaceable>META</replaceable></link>
   or: <link linkend="ocamlfind.list">ocamlfind list [-describe]</link>
   or: <link linkend="ocamlfind.printppx">ocamlfind printppx [-help | other options] <replaceable>package_name</replaceable> ...</link>
   or: <link linkend="ocamlfind.printconf">ocamlfind printconf [ variable ]</link>
   or: <link linkend="ocamlfind.pkgcmd">ocamlfind <replaceable>package</replaceable>/<replaceable>command</replaceable> <replaceable>arg</replaceable> ...</link>

Optional toolchain selection by:
  <link linkend="ocamlfind.toolchain">ocamlfind -toolchain <replaceable>name</replaceable> ...</link>
</synopsis>
</refsynopsisdiv>


<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.query">
  THE "query" SUBCOMMAND
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind query [ -predicates <replaceable>p</replaceable>  | 
                  -format <replaceable>f</replaceable> |
                  -long-format | -l |
                  -i-format |
                  -l-format | 
                  -a-format |
                  -o-format | 
	          -p-format |
                  -prefix <replaceable>p</replaceable> |
                  -separator <replaceable>s</replaceable> | 
                  -suffix <replaceable>s</replaceable> |
                  -pp |
                  -descendants | -d |
                  -recursive  | -r
                  -qe | -qo] <replaceable>package</replaceable> ...
</programlisting>
</refsect2>

<refsect2> <title>Description</title> 

<para> This command looks packages up, sorts them optionally, and
prints attributes of them. If the option -recursive (short: -r) is not
specified, exactly the packages given on the command line are looked
up; if -recursive is present, the packages and all their ancestors, or
if -descendants (short: -d) is present, too, all their descendants are printed.
</para>

<para>
Package lookup and the selection of the attributes of the packages can
be modified by specifying predicates; without a -predicates option the
empty set of predicates is used. Note that even the lookup is
influenced by the set of actual predicates as the "requires" variables
may be conditional.
</para>

<para>
What is printed about a package depends on the specified format; there
are a number of options that modify the format. Some formats denote
sets of values (such as -format %a), in which case multiple output
records are printed for every package. (It is even possible to specify
formats denoting the Cartesian product of sets, such as -format %a%o,
but this does not make sense.) Before the first output record the
prefix is printed, and the suffix after the last record. Between two
records the separator is printed.
</para>
</refsect2>

<refsect2>
<title>Options</title>

<variablelist>
<varlistentry>
<term>-predicates <replaceable>p</replaceable></term>
<listitem><para>Sets the set of actual predicates. The argument
  <replaceable>p</replaceable> is a list of predicate names separated
  by commas and/or whitespace. If multiple -predicates options are
  given, the union of all specified sets is effectively used.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-format <replaceable>f</replaceable></term>
<listitem><para>Sets the format to the string
  <replaceable>f</replaceable>. Characters preceded by a percent sign
  are interpreted as placeholders; all other characters mean
  themselves. The defined placeholders are listed below.
  The default format is "%d".
</para></listitem>
</varlistentry>
<varlistentry>
<term>-long-format or -l</term>
<listitem><para>Sets the format such that all relevant variables are printed.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-i-format</term>
<listitem><para>Same as -format "-I %d", i.e. directory options for ocamlc are printed.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-l-format</term>
<listitem><para>Same as -format "-ccopt -L%d", i.e. directory options for the
linker backend are printed.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-a-format</term>
<listitem><para>Same as -format "%+a", i.e. archive file names are printed.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-o-format</term>
<listitem><para>Same as -format "%o", i.e. linker options are printed.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-p-format</term>
<listitem><para>Same as -format "%p", i.e. package names are printed.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-prefix <replaceable>p</replaceable></term>
<listitem><para>Sets the prefix that is printed before the first output record
  to the given string. The default prefix is the empty string.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-suffix <replaceable>s</replaceable></term>
<listitem><para>Sets the suffix that is printed after the last output record
  to the given string. The default suffix is the empty string.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-separator <replaceable>s</replaceable></term>
<listitem><para>Sets the separator that is printed between output records to
  the given string. The default separator is a linefeed character.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-recursive or -r</term>
<listitem><para>Not only the packages given on the command line are queried
  but also all ancestors or descendants. If the option -descendants is
  specified, too, the descendants are printed, otherwise the
  ancestors. The packages are topologically sorted.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-descendants -d</term>
<listitem><para>Instead of the ancestors the descendants of the
  given packages are queried. This option implies <literal>-recursive</literal>.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-pp</term>
<listitem><para>Query preprocessor packages (camlp4 syntax extensions). Normally
it is not needed to set -predicates, except you need the archives (then add
-predicates byte). This option implies <literal>-recursive</literal>.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-qe</term>
<listitem><para>Do not print most errors to stderr, just set the exit code
</para></listitem>
</varlistentry>
<varlistentry>
<term>-qo</term>
<listitem><para>Do not print the regular output.
</para></listitem>
</varlistentry>

</variablelist>
</refsect2>

<refsect2>
<title>Placeholders meaningful in the -format option</title>

<variablelist>
<varlistentry>
<term>%%</term>
  <listitem><para>Replaced by a single percent sign</para></listitem>
</varlistentry>
<varlistentry>
<term>%p</term>
  <listitem><para>Replaced by the package name</para></listitem>
</varlistentry>
<varlistentry>
<term>%d</term>
  <listitem><para>Replaced by the package directory</para></listitem>
</varlistentry>
<varlistentry>
<term>%m</term>
  <listitem><para>Replaced by the path to the META file (new since findlib-1.6)
</para></listitem>
</varlistentry>
<varlistentry>
<term>%D</term>
  <listitem><para>Replaced by the package description</para></listitem>
</varlistentry>
<varlistentry>
<term>%v</term>
  <listitem><para>Replaced by the version string</para></listitem>
</varlistentry>
<varlistentry>
<term>%a</term>
  <listitem><para>Replaced by the archive filename. If there is more
  than one archive, a separate output record is printed for every archive.
</para></listitem>
</varlistentry>
<varlistentry>
<term>%+a</term>
  <listitem><para>Like %a, but the filenames are converted to absolute
  paths ("+" and "@" notations are resolved)
</para></listitem>
</varlistentry>
<varlistentry>
<term>%A</term>
  <listitem><para>Replaced by the list of archive filenames.</para></listitem>
</varlistentry>
<varlistentry>
<term>%+A</term>
  <listitem><para>Like %A, but the filenames are converted to absolute
  paths ("+" and "@" notations are resolved)
</para></listitem>
<varlistentry>
<term>%o</term>
  <listitem><para>Replaced by one linker option. If there is more than
  one option, a separate output record is printed for every option.
</para></listitem>
</varlistentry>
<varlistentry>
<term>%O</term>
  <listitem><para>Replaced by the list of linker options.</para></listitem>
</varlistentry>
<varlistentry>
<term>%(<replaceable>property</replaceable>)</term>
  <listitem><para>Replaced by the value of the property named in parentheses,
or the empty string if not defined.</para></listitem>
</varlistentry>
</variablelist>
</refsect2>

</refsect1>


<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.ocamlc">
  <anchor id="ocamlfind.ocamlcp">
  <anchor id="ocamlfind.ocamlopt">
  <anchor id="ocamlfind.ocamlmktop">
  THE SUBCOMMANDS "ocamlc", "ocamlcp", "ocamlopt", and "ocamlmktop"
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind ( ocamlc | ocamlcp | ocamlopt | ocamlmktop )
          [ -package <replaceable>package-name-list</replaceable> |
            -linkpkg |
	    -predicates <replaceable>pred-name-list</replaceable> |
            -dontlink <replaceable>package-name-list</replaceable> |
	    -syntax <replaceable>pred-name-list</replaceable> |
            -ppopt <replaceable>camlp4-arg</replaceable> |
            -ppxopt <replaceable>package</replaceable>,<replaceable>arg</replaceable> |
            -dllpath-pkg <replaceable>package-name-list</replaceable> |
            -dllpath-all |
	    -passopt <replaceable>arg</replaceable> |
            -passrest <replaceable>arg...</replaceable> |
            -only-show |
	    <replaceable>standard-option</replaceable> ]
          <replaceable>file</replaceable> ...
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>

<para>
These subcommands are drivers for the compilers with the same names,
i.e. "ocamlfind ocamlc" is a driver for "ocamlc", and so on. The
subcommands understand all documented options of the compilers (here
called <replaceable>standard-options</replaceable>), but also a few
more options. If these subcommands are invoked only with standard
options, they behave as if the underlying compiler had been called
directly. The extra options modify this.
</para>

<para>
Internally, these subcommands transform the given list of options and
file arguments into an invocation of the driven compiler. This
transformation only adds options and files, and the relative order of
the options and files passed directly is unchanged.
</para>

<para>
If there are -package options, additional directory search specifiers
will be included ("-I", and "-ccopt -I"), such that files of all named
packages and all ancestors can be found.
</para>

<para>
The -linkpkg option causes that the packages listed in the -package
options and all necessary ancestors are linked in. This means that the
archive files implementing the packages are inserted into the list of
file arguments.
</para>

<para>
As the package database is queried a set of predicates is needed. Most
predicates are set automatically, see below, but additional predicates
can be given by a -predicates option.
</para>

<para>
If there is a <literal>-syntax</literal> option, the drivers assume that
a preprocessor is to be used. In this case, the preprocessor command
is built first in a preprocessor stage, and this command is passed to the
compiler using the <literal>-pp</literal> option. The set of predicates
in the preprocessor stage is different from the set in the compiler/linker
stage.</para>

</refsect2>

<refsect2>
<title>Options for compiling and linking</title>

<para>
Here, only the additional options not interpreted by the compiler but
by the driver itself, and options with additional effects are explained.
Some options are only meaningful for the preprocessor call, and are
explained below.
</para>

<variablelist>
<varlistentry>
<term>-package <replaceable>package-name-list</replaceable></term>
  <listitem><para>Adds the listed package names to the set of included
  packages. The package names may be separated by commas and/or
  whitespace. In the transformed command, for every package of the set
  of included packages and for any ancestor a directory search option
  is inserted after the already given options. This means that
  "-I" and "-ccopt -I" options are added for every package directory.
  </para></listitem>
</varlistentry>
<varlistentry>
<term>-linkpkg</term>
  <listitem><para>Causes that in the transformed command all archives
  of the packages specified by -packages and all their ancestors are
  added to the file arguments. More precisely, these archives are
  inserted before the first given file argument. Furthermore, "-ccopt
  -L" options for all package directories, and the linker options of
  the selected packages are added, too. Note that the archives are
  inserted in topological order while the linker options are added in
  reverse toplogical order.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-predicates <replaceable>pred-name-list</replaceable></term>
  <listitem><para>Adds the given predicates to the set of actual
  predicates. The predicates must be separated by commas and/or
  whitespace. 
</para></listitem>
</varlistentry>
<varlistentry>
<term>-dontlink <replaceable>package-name-list</replaceable></term>
  <listitem><para>This option modifies the behaviour of
  -linkpkg. Packages specified here and all ancestors are not linked
  in. Again the packages are separated by commas and/or whitespace.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-dllpath-pkg <replaceable>package-name-list</replaceable></term>
  <listitem><para>For these packages <literal>-dllpath</literal> options
  are added to the compiler command. This may be useful when the ld.conf
  file is not properly configured.</para></listitem>
</varlistentry>
<varlistentry>
<term>-dllpath-all</term>
  <listitem><para>For all linked packages <literal>-dllpath</literal> options
  are added to the compiler command. This may be useful when the ld.conf
  file is not properly configured.</para></listitem>
</varlistentry>
<varlistentry>
<term>-passopt <replaceable>arg</replaceable></term>
  <listitem><para>The argument <replaceable>arg</replaceable> is
  passed directly to the underlying compiler. This is needed to
  specify undocumented compiler options.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-passrest <replaceable>arg...</replaceable></term>
  <listitem><para>All following arguments <replaceable>arg...</replaceable> are
  passed directly to the underlying compiler. This is needed to
  specify undocumented compiler options.
</para></listitem>
<varlistentry>
<term>-only-show</term>
  <listitem><para>Only prints the constructed command (ocamlc/ocamlopt) to
  stdout, but does not execute the command. (This is for the unlikely event
  that you need a wrapper around ocamlfind.)
</para></listitem>
</varlistentry>

<varlistentry>
<term>-verbose</term>
  <listitem><para>This standard option is interpreted by the driver, too.
</para></listitem>
</varlistentry>
<varlistentry>
<term>-thread</term>
  <listitem><para>This standard option causes that the predicate "mt"
  is added to the set of actual predicates. If POSIX threads are available,
  the predicate "mt_posix" is selected, too. If only VM threads are
  available, the predicate "mt_vm" is included into the set, and the
  compiler switch is changed into -vmthread.
</para>
  <para>Note that the presence of the "mt" predicate triggers special
fixup of the dependency graph (see below).</para>
</listitem>
</varlistentry>
<varlistentry>
<term>-vmthread</term>
  <listitem><para>This standard option causes that the predicates "mt"
  and "mt_vm" are added to the set of actual predicates.
</para>
  <para>Note that the presence of the "mt" predicate triggers special
fixup of the dependency graph (see below).</para>
</listitem>
</varlistentry>
<varlistentry>
<term>-p</term>
  <listitem><para>This standard option of "ocamlopt" causes that the
  predicate "gprof" is added to the set of actual predicates.
</para></listitem>
</varlistentry>

</variablelist>

</refsect2>

<refsect2>
<title>Options for preprocessing</title>

<para>
The options relevant for the preprocessor are the following:
</para>

<variablelist>
<varlistentry>
<term>-package <replaceable>package-name-list</replaceable></term>
  <listitem><para>These packages are considered while looking up the
  preprocessor arguments. (It does not cause problems that the same
  -package option is used for this purpose, because the set of predicates
  is different.) It is recommended to mention at least <literal>camlp4</literal>
  here if the preprocessor is going to be used.
  </para></listitem>
</varlistentry>
<varlistentry>
<term>-syntax <replaceable>pred-name-list</replaceable></term>
  <listitem><para>These predicates are assumed to be true in addition
  to the standard preprocessor predicates. See below for a list.
</para></listitem>
</varlistentry>
<varlistentry>
  <term>-ppopt <replaceable>camlp4-arg</replaceable></term>
  <listitem><para>This argument is passed to the camlp4 call.
  </para></listitem>
</varlistentry>
<varlistentry>
  <term>-ppxopt <replaceable>package</replaceable>,<replaceable>arg</replaceable></term>
  <listitem><para>Add <replaceable>arg</replaceable> to the ppx
      preprocessor invocation specified via the "ppx" property in
      the META file of <replaceable>package</replaceable>.
  </para></listitem>
</varlistentry>
</variablelist>
</refsect2>

<refsect2>
<title>Predicates for compiling and linking</title>

<variablelist>
<varlistentry>
<term>byte</term>
<listitem>
<para>
The "byte" predicate means that one of the bytecode compilers is
used. It is automatically included into the predicate set if the
"ocamlc", "ocamlcp", or "ocamlmktop" compiler is used.
</para>
</listitem>
</varlistentry>

<varlistentry>
<term>native</term>
<listitem>
<para>
The "native" predicate means that the native compiler is used. It is
automatically included into the predicate set if the "ocamlopt"
compiler is used.
</para>
</listitem>
</varlistentry>

<varlistentry>
<term>toploop</term>
<listitem>
<para>
The "toploop" predicate means that the toploop is available in the
linked program. This predicate is only set when the toploop is actually
being executed, not when the toploop is created (this changed in version
1.0.4 of findlib).
</para>
</listitem>
</varlistentry>

<varlistentry>
<term>create_toploop</term>
<listitem>
<para>
This predicate means that a toploop is being created (using
ocamlmktop).
</para>
</listitem>
</varlistentry>

<varlistentry>
<term>mt</term>
<listitem>
<para>
The "mt" predicate means that the program is multi-threaded. It is
automatically included into the predicate set if the -thread option is
given. 
</para>
</listitem>
</varlistentry>

<varlistentry>
<term>mt_posix</term>
<listitem>
<para>
The "mt_posix" predicate means that in the case "mt" is set, too, the
POSIX libraries are used to implement threads. "mt_posix" is automatically
included into the predicate set if the variable "type_of_threads" in the
META description of the "threads" package has the value "posix". This
is normally the case if "findlib" is configured for POSIX threads.
</para>
</listitem>
</varlistentry>

<varlistentry>
<term>mt_vm</term>
<listitem>
<para>
The "mt_vm" predicate means that in the case "mt" is set, too, the
VM thread emulation is used to implement multi-threading.
</para>
</listitem>
</varlistentry>

<varlistentry>
<term>gprof</term>
<listitem>
<para>
The "gprof" predicate means that in the case "native" is set, too, the
program is compiled for profiling. It is automatically included into
the predicate set if "ocamlopt" is used and the -p option is in
effect.
</para>
</listitem>
</varlistentry>

<varlistentry>
<term>autolink</term>
<listitem>
<para>
The "autolink" predicate means that ocamlc is able to perform automatic
linking. It is automatically included into the predicate set if ocamlc
knows automatic linking (from version 3.00), but it is not set if the
-noautolink option is set.
</para>
</listitem>
</varlistentry>

<varlistentry>
  <term>syntax</term>
  <listitem><para>This predicate is set if there is a <literal>-syntax</literal>
  option. It is set both for the preprocessor and the compiler/linker stage,
  and it can be used to find out whether the preprocessor is enabled or not.
  </para></listitem>
</varlistentry>

</variablelist>

</refsect2>

<refsect2>
<title>Predicates for preprocessing</title>

<variablelist>
<varlistentry>
  <term>preprocessor</term>
  <listitem><para>This predicate is always set while looking up the
  preprocessor arguments. It can be used to distinguish between the
  preprocessor stage and the compiler/linker stage.</para></listitem>
</varlistentry>
<varlistentry>
  <term>syntax</term>
  <listitem><para>This predicate is set if there is a <literal>-syntax</literal>
  option. It is set both for the preprocessor and the compiler/linker stage,
  and it can be used to find out whether the preprocessor is enabled or not.
  </para></listitem>
</varlistentry>
<varlistentry>
  <term>camlp4o</term>
  <listitem><para>This is the reserved predicate for the standard O'Caml syntax.
  It can be used in the <literal>-syntax</literal> predicate list.
  </para></listitem>
</varlistentry>
<varlistentry>
  <term>camlp4r</term>
  <listitem><para>This is the reserved predicate for the revised O'Caml syntax.
  It can be used in the <literal>-syntax</literal> predicate list.
  </para></listitem>
</varlistentry>
</variablelist>
</refsect2>


<refsect2>
<title>Special behaviour of "ocamlmktop"</title>

<para> As there is a special module <literal>Topfind</literal> that
supports loading of packages in scripts, the "ocamlmktop" subcommand
can add initialization code for this module.  This extra code is
linked into the executable if "findlib" is in the set of effectively
linked packages.  
</para>

</refsect2>


<refsect2>
<title>Fixup of the dependency graph for multi-threading</title>
<para>For a number of reasons the presence of the "mt" predicate triggers
that (1) the package "threads" is added to the list of required packages
and (2) the package "threads" becomes prerequisite of all other packages
(except of itself and a few hardcoded exceptions). The effect is that
the options -thread and -vmthread automatically select the "threads"
package, and that "threads" is inserted at the right position in the
package list.</para>
</refsect2>


<refsect2>
<title>Extended file naming</title>
<para>At a number of places one can not only refer to files by absolute
or relative path names, but also by extended names. These have two
major forms: "+<replaceable>name</replaceable>"
refers to the subdirectory <replaceable>name</replaceable> of the
standard library directory, and "@<replaceable>name</replaceable>"
refers to the package directory of the package <replaceable>name</replaceable>.
Both forms can be continued by a path, e.g. "@netstring/netstring_top.cma".
</para>

<para>
You can use extended names: (1) With <literal>-I</literal> options,
(2) as normal file arguments of the compiler, (3) in the 
"archive" property of packages.
</para>
</refsect2>


<refsect2>
<title>How to set the names of the compiler executables</title>

<para> Normally, the O'Caml bytecode compiler can be called under the name
<literal>ocamlc</literal>. However, this is not always true; sometimes a
different name is chosen.</para>

<para> You can instruct ocamlfind to call executables with other names than
<literal>ocamlc</literal>, <literal>ocamlopt</literal>,
<literal>ocamlmktop</literal>, and <literal>ocamlcp</literal>. If present,
the environment variable <literal>OCAMLFIND_COMMANDS</literal> is interpreted
as a mapping from the standard names to the actual names of the executables. It
must have the following format:

<programlisting>
<replaceable>standardname1</replaceable>=<replaceable>actualname1</replaceable> <replaceable>standardname2</replaceable>=<replaceable>actualname2</replaceable> ...
</programlisting>
</para>

<para>Example: You may set <literal>OCAMLFIND_COMMANDS</literal> as follows:

<programlisting>
OCAMLFIND_COMMANDS='ocamlc=ocamlc-3.00 ocamlopt=ocamlopt-3.00'
export OCAMLFIND_COMMANDS
</programlisting>
</para>

<para>Alternatively, you can change the configuration file
<link linkend="findlib.conf">findlib.conf</link>.</para>
</refsect2>

</refsect1>

<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.ocamlmklib">
  THE SUBCOMMAND "ocamlmklib"
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind ocamlmklib
          [ -package <replaceable>package-name-list</replaceable> |
	    -predicates <replaceable>pred-name-list</replaceable> |
            -dllpath-pkg <replaceable>package-name-list</replaceable> |
            -dllpath-all |
	    -passopt <replaceable>arg</replaceable> |
            -passrest <replaceable>arg...</replaceable> |
	    <replaceable>standard-option</replaceable> ]
            <replaceable>file</replaceable> ...
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>

<para>
This is a wrapper around ocamlmklib, and creates library archives and
DLLs. In addition to the standard options, one can use -package to
add the search path of packages. Note that no predicates are set by default -
the wrapper does not know whether this is about byte or native code linking.
</para>

<para>
This wrapper is mostly provided for completeness.
</para>
</refsect2>

</refsect1>



<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.ocamldep">
  THE "ocamldep" SUBCOMMAND
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind ocamldep [-package <replaceable>package-name-list</replaceable> |
                    -syntax <replaceable>pred-name-list</replaceable> |
                    -ppopt <replaceable>camlp4-arg</replaceable> |
                    -passopt <replaceable>arg</replaceable> |
                    -passrest <replaceable>arg...</replaceable> |
                    -verbose |
                    <replaceable>standard-option</replaceable>] <replaceable>file</replaceable> ...
</programlisting>
</refsect2>

<refsect2><title>Description</title>
<para>
This command is a driver for the tool <literal>ocamldep</literal> of the
O'Caml distribution. This driver is only useful in conjunction with
the preprocessor camlp4; otherwise it does not provide more functions
than <literal>ocamldep</literal> itself.
</para>
</refsect2>

<refsect2>
<title>Options</title>

<para>
Here, only the additional options not interpreted by <literal>ocamldep</literal>
but
by the driver itself, and options with additional effects are explained.
</para>

<variablelist>
<varlistentry>
  <term>-package <replaceable>package-name-list</replaceable></term>
  <listitem><para>The packages named here are only used to look up the
preprocessor options. The package <literal>camlp4</literal> should be
specified anyway, but further packages that add capabilities to the
preprocessor can also be passed.</para></listitem>
</varlistentry>

<varlistentry>
  <term>-syntax <replaceable>pred-name-list</replaceable></term>
  <listitem><para>The predicates that are in effect during the look-up
of the preprocessor options. At least, either <literal>camlp4o</literal>
(selecting the normal syntax), or <literal>camlp4r</literal> (selecting
the revised syntax) should be specified.</para></listitem>
</varlistentry>

<varlistentry>
  <term>-ppopt <replaceable>camlp4-arg</replaceable></term>
  <listitem><para>An option that is passed through to the camlp4 call.</para>
</listitem>
</varlistentry>

<varlistentry>
  <term>-passopt <replaceable>arg</replaceable></term>
  <listitem><para>An option that is passed through to the ocamldep call.</para>
</listitem>
</varlistentry>

<varlistentry>
  <term>-passrest <replaceable>arg...</replaceable></term>
  <listitem><para>All further arguments are passed down to ocamldep
  unprocessed</para>
</listitem>
</varlistentry>

<varlistentry>
  <term>-verbose</term>
  <listitem><para>Displays the resulting ocamldep command (for debugging)
</para></listitem>
</varlistentry>
</variablelist>

</refsect2>

<refsect2>
<title>Example</title>

<para>A typical way of using this driver:

<programlisting>
ocamlfind ocamldep -package camlp4,xstrp4 -syntax camlp4r file1.ml file2.ml
</programlisting>

This command outputs the dependencies of <literal>file1.ml</literal> and
<literal>file2.ml</literal>, although these modules make use of the
syntax extensions provided by <literal>xstrp4</literal> and are written
in revised syntax.
</para>
</refsect2>

</refsect1>


<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.ocamlbrowser">
  THE "ocamlbrowser" SUBCOMMAND
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind ocamlbrowser [-package <replaceable>package-name-list</replaceable> |
                        -all |
                        -passopt <replaceable>arg</replaceable> 
                        -passrest ]
</programlisting>
</refsect2>

<refsect2><title>Description</title>
<para>
This driver calls the <literal>ocamlbrowser</literal> with package options.
With <literal>-package</literal>, the specified packages are included into
the search path of the browser, and the modules of these packages become
visible (in addition to the standard library). The option <literal>-all</literal> causes that all packages are selected that are managed by findlib.</para>

<para>
As for other drivers, the options <literal>-passopt</literal> and
<literal>-passrest</literal> can be used
to pass arguments directly to the <literal>ocamlbrowser</literal> program.
</para>
</refsect2>
</refsect1>

<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.ocamldoc">
  THE SUBCOMMAND "ocamldoc"
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind ocamldoc
          [ -package <replaceable>package-name-list</replaceable> |
	    -predicates <replaceable>pred-name-list</replaceable> |
	    -syntax <replaceable>pred-name-list</replaceable> |
            -ppopt <replaceable>camlp4-arg</replaceable> |
	    <replaceable>standard-option</replaceable> ]
          <replaceable>file</replaceable> ...
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>

<para>
This subcommand is a driver for ocamldoc. It undestands all options
ocamldoc supports plus the mentioned findlib options. Basically,
the -package options are translated into -I options, and the selected
syntax options are translated into camlp4 options.
</para>
</refsect2>

<refsect2>
<title>Options</title>

<para>
Here, only the additional options not interpreted by <literal>ocamldep</literal>
but
by the driver itself, and options with additional effects are explained.
</para>

<variablelist>
<varlistentry>
<term>-package <replaceable>package-name-list</replaceable></term>
  <listitem><para>Adds the listed package names to the set of included
  packages. The package names may be separated by commas and/or
  whitespace. In the transformed command, for every package of the set
  of included packages and for any ancestor a directory search option
  is inserted after the already given options. This means that
  "-I" options are added for every package directory.
  </para></listitem>
</varlistentry>

<varlistentry>
<term>-predicates <replaceable>pred-name-list</replaceable></term>
  <listitem><para>Adds the given predicates to the set of actual
  predicates. The predicates must be separated by commas and/or
  whitespace. 
</para></listitem>
</varlistentry>

<varlistentry>
  <term>-syntax <replaceable>pred-name-list</replaceable></term>
  <listitem><para>The predicates that are in effect during the look-up
of the preprocessor options. At least, either <literal>camlp4o</literal>
(selecting the normal syntax), or <literal>camlp4r</literal> (selecting
the revised syntax) should be specified.</para></listitem>
</varlistentry>

<varlistentry>
  <term>-ppopt <replaceable>camlp4-arg</replaceable></term>
  <listitem><para>An option that is passed through to the camlp4 call.</para>
</listitem>
</varlistentry>
</variablelist>
</refsect2>
</refsect1>


<!-- ********************************************************************** -->

<!--

<refsect1>
<title><anchor id="ocamlfind.use">
  THE "use" SUBCOMMAND
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind use [-p <replaceable>prefix</replaceable>] <replaceable>package_name</replaceable> ...
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>

<para>
This subcommand should not be used any longer. It is equivalent to the
"query" subcommand with -i-format and -separator " ".
</para>
</refsect2>
</refsect1>

-->

<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.install">
  THE "install" SUBCOMMAND
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind install [ -destdir <replaceable>directory</replaceable> ]
                  [ -metadir <replaceable>directory</replaceable> ]
	          [ -ldconf <replaceable>path</replaceable> ]
                  [ -dont-add-directory-directive ]
                  [ -patch-version <replaceable>string</replaceable> ]
                  [ -patch-rmpkg <replaceable>name</replaceable> ]
                  [ -patch-archives ]
	          [ -dll ] [ -nodll ] [ -optional ] [ -add ]
                  <replaceable>package_name</replaceable> <replaceable>file</replaceable> ...
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>
<para>
This subcommand installs a new package either at the default location
(see the variable <literal>destdir</literal> of
<link linkend="findlib.conf">findlib.conf</link>), or in the directory
specified by the -destdir option. This
means that a new package directory is created and that the files on
the command line are copied to this directory. It is required that a
<literal>META</literal> file is one of the files copied to the target
directory.
</para>

<para>
Note that package directories should be flat (no
subdirectories). Existing packages are never overwritten.
</para>

<para>
It is possible to have a separate directory for all the META files. If
you want that, you have either to set the variable
<literal>metadir</literal> of 
<link linkend="findlib.conf">findlib.conf</link>, or to specify the
-metadir option. In this case, the file called META is copied to the
specified directory and renamed to META.p (where p is the package
name), while all the other files are copied to the package
directory as usual. Furthermore, the META file is modified such that the
<literal>directory</literal> variable contains the path of the package
directory. 
</para>

<para> 
The option -dont-add-directory-directive prevents the installer from
adding a <literal>directory</literal> variable.
</para>

<para>
If there are files ending in the suffixes <literal>.so</literal> or
<literal>.dll</literal>, the package directory will be added to the
DLL configuration file <literal>ld.conf</literal>, such that the dynamic
loader can find the DLL. The location of this file can be overriden by
the -ldconf option. To turn this feature off, use "-ldconf ignore";
this causes that the ld.conf file is not modified.
</para>

<para>
However, if there is a stublibs directory in site-lib, the DLLs are not
installed in the package directory, but in this directory that is
shared by all packages that are installed at the same location.
In this case, the configuration file <literal>ld.conf</literal> is
not modified, so you do not need to say "-ldconf ignore" if you
prefer this style of installation.
</para>

<para>
The options -dll and -nodll can be used to control exactly which files
are considered as DLLs and which not. By default, the mentioned
suffix rule is in effect: files ending in ".so" (Unix) or ".dll"
(Windows) are DLLs. The switch -dll changes this, and all following
files are considered as DLLs, regardless of their suffix. The switch 
-nodll expresses that the following files are not DLLs, even if they
have a DLL-like suffix. For example, in the following call the files
f1 and f2 are handled by the suffix rule; f3 and f4 are DLLs anyway;
and f5 and f6 are not DLLs:

<programlisting>
ocamlfind install p f1 f2 -dll f3 f4 -nodll f5 f6
</programlisting>
</para>

<para>
The switch -optional declares that all following files are optional,
i.e. the command will not fail if files do not exist.
</para>

<para>
The -patch options may be used to change the contents of the META files
while it is being installed. The option -patch-version changes the
contents of the top-level "version" variable. The option -patch-rmpkg
removes the given subpackage. The option -patch-archives is experimental,
in particular it removes all non-existing files from "archive" variables,
and even whole subpackages if the archives are missing.
</para>

<para>
The effect of -add is to add further files to an already installed
packages.
</para>

</refsect2>

</refsect1>


<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.remove">
  THE "remove" SUBCOMMAND
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind remove [ -destdir <replaceable>directory</replaceable> ]
                 [ -metadir <replaceable>directory</replaceable> ]
                 [ -ldconf <replaceable>path</replaceable> ]
                 <replaceable>package_name</replaceable>
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>
<para>
The package will removed if it is installed at the default location 
(see the variable <literal>destdir</literal> of
<link linkend="findlib.conf">findlib.conf</link>). If the package
resides at a different location, it will not be removed by default;
however, you can pass an alternate directory for packages by the
-destdir option. (This must be the same directory as specified at
installation time.)
</para> 

<para>
Note that package directories should be flat (no subdirectories); this
subcommand cannot remove deep package directories. 
</para>

<para>
If you have a separate directory for META files, you must either
configure this directory by the <literal>metadir</literal> variable
of <link linkend="findlib.conf">findlib.conf</link>, or by specifying
the -metadir option.
</para>

<para> 
The command does not fail if the package and/or the META
file cannot be located. You will get a warning only in this case.
</para>

<para>
If the package directory is mentioned in the <literal>ld.conf</literal>
configuration file for DLLs, it will be tried to remove this entry
from the file. The location of this file can be overriden by
the -ldconf option. To turn this feature off, use "-ldconf ignore";
this causes that the ld.conf file is not modified.
</para>

<para>
If there is a stublibs directory, it is checked whether the package
owns any of the files in this directory, and the owned files will
be deleted.
</para>

</refsect2>
</refsect1>


<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.list">
  THE "list" SUBCOMMAND
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind list [-describe]
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>
<para>
This command lists all packages in the search path. The option -describe
outputs the package descriptions, too.
</para>
</refsect2>
</refsect1>

<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.printppx">
  THE "printppx" SUBCOMMAND
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind printppx
          [ -predicates <replaceable>pred-name-list</replaceable> ]
          [ -ppxopt <replaceable>package</replaceable>,<replaceable>arg</replaceable> ]
          <replaceable>package</replaceable> ...
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>
<para>
This command prints the ppx preprocessor options as they would
occur in an OCaml compiler invocation for the packages listed in
the command. The output includes one "-ppx" option for each
preprocessor. The possible options have the same meaning as for
"ocamlfind ocamlc". The option "-predicates" adds assumed
predicates and
"-ppxopt <replaceable>package</replaceable>,<replaceable>arg</replaceable>"
adds "<replaceable>arg</replaceable>" to the ppx invocation of
package <replaceable>package</replaceable>.
</para>

<para>
The output of "ocamlfind printppx" will contain quotes
"<literal>"</literal>" for ppx commands that contain
space-separated arguments. In this case <literal>$(ocamlfind
printppx ...)</literal> won't work as naively expected, because
many shells (including bash and dash) perform field splitting on
the result of command substitutions without honoring quotes.
</para>
</refsect2>
</refsect1>

<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.lint">
  THE "lint" SUBCOMMAND
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind lint <replaceable>file</replaceable>
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>
<para>
Checks the META file, and reports possible problems.
</para>
</refsect2>
</refsect1>

<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.printconf">
  THE "printconf" SUBCOMMAND
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind printconf [ conf | path | destdir | metadir | metapath | stdlib | ldconf ]
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>
<para>
This command prints the effective configuration after reading the
configuration file, and after applying the various environment
variables overriding settings. When called without arguments, the command
prints all configuration options in a human-readable form. When called
with an argument, only the value of the requested option is printed without
explaining texts:
</para>

<variablelist>
<varlistentry>
  <term><literal>conf</literal></term>
  <listitem><para>Prints the location of the configuration file findlib.conf
  </para></listitem>
</varlistentry>
<varlistentry>
  <term><literal>path</literal></term>
  <listitem><para>Prints the search path for packages. The members of the
  path are separated by linefeeds.</para></listitem>
</varlistentry>
<varlistentry>
  <term><literal>destdir</literal></term>
  <listitem><para>Prints the location where package are installed and
  removed by default.</para></listitem>
</varlistentry>
<varlistentry>
  <term><literal>metadir</literal></term>
  <listitem><para>Prints the location where META files are installed and
  removed (if the alternative layout is used).</para></listitem>
</varlistentry>
<varlistentry>
  <term><literal>metapath</literal></term>
  <listitem><para>Prints the path where the META file is installed for
a fictive package. The name of the package is marked with '%s' in the
path. For instance, this command could output "/some/path/%s/META" or
"/some/path/META.%s", depending on the layout.</para></listitem>
</varlistentry>
<varlistentry>
  <term><literal>stdlib</literal></term>
  <listitem><para>Prints the location of the standard library.
</para></listitem>
</varlistentry>
<varlistentry>
  <term><literal>ldconf</literal></term>
  <listitem><para>Prints the location of the ld.conf file
</para></listitem>
</varlistentry>
</variablelist>

</refsect2>
</refsect1>


<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.pkgcmd">
  THE SUBCOMMAND CALLING PACKAGE PROGRAMS
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind <replaceable>pkg</replaceable>/<replaceable>cmd</replaceable> <replaceable>argument</replaceable> ...
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>
<para>This subcommand is useful to call programs that are installed in
package directories. It looks up the directory for 
<replaceable>pkg</replaceable> and calls the command named
<replaceable>cmd</replaceable> in this directory. The remaining arguments
are passed to this command.</para>

<para>argv(0) contains the absolute path to the command, and argv(1) and
the following argv entries contain the arguments. The working directory
is not changed.</para>

<para>Example: To call the program "x" that is installed in package "p",
with arguments "y" and "z", run:</para>

<programlisting>
ocamlfind p/x y z
</programlisting>

</refsect2>
</refsect1>


<refsect1>
<title>
  CONFIGURATION FILE, ENVIRONMENT VARIABLES
</title>

<para> The configuration file and environment variables are documented
in the manual page for
 <link linkend="findlib.conf">findlib.conf</link>.
</para>
</refsect1>

<!-- ********************************************************************** -->

<refsect1>
<title><anchor id="ocamlfind.toolchain">
  HOW TO SET THE TOOLCHAIN
</title>

<refsect2>
<title>Synopsis</title>
<programlisting>
ocamlfind -toolchain <replaceable>name</replaceable> ...
</programlisting>
</refsect2>

<refsect2>
<title>Description</title>
<para>The -toolchain option can be given before any other command,
e.g.
<programlisting>
ocamlfind -toolchain foo ocamlc -c file.ml
</programlisting>
compiles file.ml with toolchain "foo". By selecting toolchains one
can switch to different command sets. For instance, the toolchain
"foo" may consist of a patched ocamlc compiler. 
See <link linkend="findlib.conf">findlib.conf</link> how to
configure toolchains.
</para>
</refsect2>
</refsect1>

</refentry>




