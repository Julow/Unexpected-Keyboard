<refentry>

<refmeta>
  <refentrytitle>Topfind</refentrytitle>
  <manvolnum>3</manvolnum>
  <refmiscinfo>The findlib package manager for OCaml</refmiscinfo>
</refmeta>

<refnamediv id="Topfind">
  <refname>Topfind</refname>
  <refpurpose>[Module to load packages into toploops]</refpurpose>
</refnamediv>


<refsynopsisdiv>
<title>SIGNATURE</title>
<synopsis>
module Topfind :
  sig
    <link linkend="Topfind.predicates"
          endterm="val.Topfind.predicates"></link>
    <link linkend="Topfind.add-predicates"
          endterm="val.Topfind.add-predicates"></link>
    <link linkend="Topfind.syntax"
          endterm="val.Topfind.syntax"></link>
    <link linkend="Topfind.standard-syntax"
          endterm="val.Topfind.standard-syntax"></link>
    <link linkend="Topfind.revised-syntax"
          endterm="val.Topfind.revised-syntax"></link>
    <link linkend="Topfind.dont-load"
          endterm="val.Topfind.dont-load"></link>
    <link linkend="Topfind.dont-load-deeply"
          endterm="val.Topfind.dont-load-deeply"></link>
    <link linkend="Topfind.load"
          endterm="val.Topfind.load"></link>
    <link linkend="Topfind.load-deeply"
          endterm="val.Topfind.load-deeply"></link>
    <link linkend="Topfind.reset"
          endterm="val.Topfind.reset"></link>
  end
</synopsis>
</refsynopsisdiv>


<refsect1>
<title>DIRECTIVES</title>
<programlisting>
<link linkend="Topfind.require"
      endterm="val.Topfind.require"></link>
<link linkend="Topfind.camlp4o"
      endterm="val.Topfind.camlp4o"></link>
<link linkend="Topfind.camlp4r"
      endterm="val.Topfind.camlp4r"></link>
<link linkend="Topfind.list"
      endterm="val.Topfind.list"></link>
</programlisting>
</refsect1>


<refsect1>
<title>PACKAGING</title>

<para>
The Topfind module is part of the "findlib" package. The module
depends on the presence of a toploop. When building a toploop, it is
automatically linked in if "findlib" is linked in, e.g.
</para>

<programlisting>
ocamlfind ocamlmktop <replaceable>options</replaceable> -package findlib -linkpkg <replaceable>options</replaceable>
</programlisting>

<para>
When the platform supports DLLs, another possibility to get a toploop
with findlib directives is to load the file "topfind" (normally installed
in the standard library directory):
</para>

<programlisting>
~ > ocaml
        Objective Caml version 3.04

# #use "topfind";;
Findlib has been successfully loaded. Additional directives:
  #require "package";;      to load a package
  #list;;                   to list the available packages
  #camlp4o;;                to load camlp4 (standard syntax)
  #camlp4r;;                to load camlp4 (revised syntax)
  Topfind.reset();;         to force that packages will be reloaded

- : unit = ()
# _
</programlisting>

<para>
This works even in scripts (but the startup message is suppressed in this
case).
</para>

<para>
The module is not thread-safe; if used in a multi-threaded script, all
packgage loading must have happened before the first thread forks.
</para>
</refsect1>


<refsect1>
<title>DESCRIPTION</title>

<para>
The Topfind module contains some functions simplifying package loading
in scripts. Most important, there is a new directive "#require" for
the same purpose.
</para>

<para>
The Topfind module needs some initialization, in particular the <link
linkend="Topfind.predicates">predicates</link> variable needs to be
set, and the packages already compiled into the toploop needs to be
declared by the <link linkend="Topfind.dont-load">don't_load</link>
function. If the toploop has been built by <xref
linkend="ocamlfind" endterm="ocamlfind">, the necessary initialization is
automatically compiled in.
</para>


<refsect2>
<title><anchor id="Topfind.predicates">
The variable Topfind.predicates
</title>
<programlisting id="val.Topfind.predicates">
val predicates : string list ref
</programlisting>
<para>
The variable contains the set of predicates that is assumed when
packages are loaded.
</para>
</refsect2>


<refsect2>
<title><anchor id="Topfind.add-predicates">
Topfind.add_predicates <replaceable>predlist</replaceable>
</title>
<programlisting id="val.Topfind.add-predicates">
val add_predicates : string list -&gt; unit
</programlisting>
<para>
This function adds the passed predicates <replaceable>predlist</replaceable>
to the variable
<literal>predicates</literal>.
</para>
</refsect2>


<refsect2>
<title><anchor id="Topfind.syntax">
Topfind.syntax <replaceable>variant</replaceable>
</title>
<programlisting id="val.Topfind.syntax">
val syntax : string -&gt; unit
</programlisting>
<para>
This function emulates the <literal>-syntax</literal> command line
switch of ocamlfind.
</para>
</refsect2>


<refsect2>
<title><anchor id="Topfind.standard-syntax">
Topfind.standard_syntax ()
</title>
<programlisting id="val.Topfind.standard-syntax">
val standard_syntax : unit -&gt; unit
</programlisting>
<para>
The same as <literal>syntax "camlp4o"</literal>.
</para>
</refsect2>


<refsect2>
<title><anchor id="Topfind.revised-syntax">
Topfind.revised_syntax ()
</title>
<programlisting id="val.Topfind.revised-syntax">
val revised_syntax : unit -&gt; unit
</programlisting>
<para>
The same as <literal>syntax "camlp4r"</literal>.
</para>
</refsect2>


<refsect2>
<title><anchor id="Topfind.dont-load">
Topfind.don't_load <replaceable>pkglist</replaceable>
</title>
<programlisting id="val.Topfind.dont-load">
val don't_load : string list -&gt; unit
</programlisting>
<para>
Declares the packages enumerated in <replaceable>pkglist</replaceable>
as being linked into the toploop.
</para>
</refsect2>


<refsect2>
<title><anchor id="Topfind.dont-load-deeply">
Topfind.don't_load_deeply <replaceable>pkglist</replaceable>
</title>
<programlisting id="val.Topfind.dont-load-deeply">
val don't_load_deeply : string list -&gt; unit
</programlisting>
<para>
Declares the packages enumerated in <replaceable>pkglist</replaceable>
and all direct and indirect ancestors as being linked into the toploop.
</para>
</refsect2>


<refsect2>
<title><anchor id="Topfind.load">
Topfind.load <replaceable>pkglist</replaceable>
</title>
<programlisting id="val.Topfind.load">
val load : string list -&gt; unit
</programlisting>
<para>
The packages enumerated in <replaceable>pkglist</replaceable> are
loaded in turn; packages that have already been loaded or that have
been declared as linked in are skipped.
</para>
</refsect2>


<refsect2>
<title><anchor id="Topfind.load-deeply">
Topfind.load_deeply <replaceable>pkglist</replaceable>
</title>
<programlisting id="val.Topfind.load-deeply">
val load_deeply : string list -&gt; unit
</programlisting>
<para>
The packages enumerated in <replaceable>pkglist</replaceable> and all
direct or indirect ancestors are loaded in topological order;
packages that have already been loaded or that have
been declared as linked in are skipped.
</para>
</refsect2>


<refsect2>
<title><anchor id="Topfind.reset">
Topfind.reset ()
</title>
<programlisting id="val.Topfind.reset">
val reset : unit -&gt; unit
</programlisting>
<para>
This function causes that Topfind forgets that any package has already
been loaded. Infomation about packages linked into the toploop remain
intact. The effect of this function is that all dynamically loaded
packages will be loaded again when <link
linkend="Topfind.load">load</link>, <link
linkend="Topfind.load-deeply">load_deeply</link> functions, or the
<link linkend="Topfind.require">#require</link> directive are executed.
</para>
</refsect2>



<refsect2>
<title><anchor id="Topfind.require">
#require "<replaceable>package-name-list</replaceable>";;
</title>
<programlisting id="val.Topfind.require">
#require "<replaceable>package-name-list</replaceable>";;
</programlisting>
<para>
The argument of the directive is a list of package names, separated by
commas and/or whitespace. The directive has the same effect as <link
linkend="Topfind.load-deeply">load_deeply</link>, i.e. the listed
packages  and all
direct or indirect ancestors are loaded in topological order;
packages that have already been loaded or that have
been declared as linked in are skipped.
</para>
</refsect2>


<refsect2>
<title><anchor id="Topfind.camlp4o">
#camlp4o ;;
</title>
<programlisting id="val.Topfind.camlp4o">
#camlp4o ;;
</programlisting>
<para>
Selects the standard syntax and loads the camlp4 preprocessor.
</para>
</refsect2>


<refsect2>
<title><anchor id="Topfind.camlp4r">
#camlp4r ;;
</title>
<programlisting id="val.Topfind.camlp4r">
#camlp4r ;;
</programlisting>
<para>
Selects the revised syntax and loads the camlp4 preprocessor.
</para>
</refsect2>

<refsect2>
<title><anchor id="Topfind.list">
#list ;;
</title>
<programlisting id="val.Topfind.list">
#list ;;
</programlisting>
<para>
Lists the packages that are in the search path.
</para>
</refsect2>

</refsect1>

</refentry>
