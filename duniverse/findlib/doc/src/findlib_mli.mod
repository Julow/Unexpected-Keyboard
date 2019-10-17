<refentry>

<refmeta>
  <refentrytitle>Findlib</refentrytitle>
  <manvolnum>3</manvolnum>
  <refmiscinfo>The findlib package manager for OCaml</refmiscinfo>
</refmeta>

<refnamediv id="Findlib">
  <refname>Findlib</refname>
  <refpurpose>[Module for package management]</refpurpose>
</refnamediv>


<refsynopsisdiv>
<title>SIGNATURE</title>
<synopsis>
module Findlib :
  sig
    <link linkend="Findlib.package-directory"
          endterm="val.Findlib.package-directory"></link>
    <link linkend="Findlib.package-property"
          endterm="val.Findlib.package-property"></link>
    <link linkend="Findlib.package-ancestors"
          endterm="val.Findlib.package-ancestors"></link>
    <link linkend="Findlib.package-deep-ancestors"
          endterm="val.Findlib.package-deep-ancestors"></link>
    <link linkend="Findlib.default-location"
          endterm="val.Findlib.default-location"></link>

    (* Note: This signature is incomplete. See findlib.mli for the
     * full signature.
     *)

  end
</synopsis>
</refsynopsisdiv>


<refsect1>
<title>PACKAGING</title>

<para>
The Findlib module is part of the "findlib" package. In order to link
it in, it is sufficient to link "findlib" in, e.g.
</para>

<programlisting>
ocamlfind ocamlc <replaceable>options</replaceable> -package findlib -linkpkg <replaceable>options</replaceable>
</programlisting>

<para>
There are archives for the bytecode compiler and for the native
compiler; for single-threaded and for multi-threaded applications, and
there is a special addition for toploops.
</para>
</refsect1>


<refsect1>
<title>DESCRIPTION</title>

<para>
The Findlib module is the primary interface of the findlib library. It
contains functions to lookup packages, to interpret <xref
linkend="META" endterm="META"> files, and to determine the ancestors of
packages.
</para>


<refsect2>
<title><anchor id="Findlib.package-directory">
Findlib.package_directory <replaceable>pkg</replaceable></title>
<programlisting id="val.Findlib.package-directory">
val package_directory : string -&gt; string
</programlisting>
<para>
Gets the absolute path of the directory where the package
<replaceable>pkg</replaceable> is stored. The exception Not_found is
raised if the package could not be found.
Other exceptions may occur as file I/O is done.
</para>
</refsect2>



<refsect2>
<title><anchor id="Findlib.package-property">
Findlib.package_property <replaceable>predicates</replaceable>
<replaceable>pkg</replaceable> <replaceable>variable</replaceable>
</title>
<programlisting id="val.Findlib.package-property">
val package_property : string list -&gt; string -&gt; string -&gt; string
</programlisting>
<para>
Determines the value of the <replaceable>variable</replaceable>
defined in the <xref linkend="META" endterm="META"> file of package
<replaceable>pkg</replaceable> with the given set of
<replaceable>predicates</replaceable>. The exception Not_found is
raised if the package or the variable could not be found.  Other
exceptions may occur as file I/O is done.
</para>
<refsect3>
<title>Examples</title>
<para>
Get the value of the "requires" variable of package "p" with an empty
set of predicates:
</para>
<programlisting>
Findlib.package_property [] "p" "requires"
</programlisting>
<para>
Get the value of the "archive" variable of package "p" for
multi-threaded bytecode applications:
</para>
<programlisting>
Findlib.package_property [ "mt"; "byte" ] "p" "archive"
</programlisting>
</refsect3>
</refsect2>



<refsect2>
<title><anchor id="Findlib.package-ancestors">
Findlib.package_ancestors <replaceable>predicates</replaceable>
  <replaceable>pkg</replaceable>
</title>
<programlisting id="val.Findlib.package-ancestors">
val package_ancestors : string list -&gt; string -&gt; string list
</programlisting>
<para>
Determines the direct ancestors of package
<replaceable>pkg</replaceable> for the set of
<replaceable>predicates</replaceable>. The returned list has no
specific order. The exception Not_found is raised if the package could
not be found. The exception Failure is raised if one of the ancestors
could not be found, or if a circular dependency has been
detected. Other exceptions may occur as file I/O is done.
</para>
</refsect2>



<refsect2>
<title><anchor id="Findlib.package-deep-ancestors">
Findlib.package_deep_ancestors <replaceable>predicates</replaceable>
  <replaceable>pkglist</replaceable>
</title>
<programlisting id="val.Findlib.package-deep-ancestors">
val package_deep_ancestors : string list -&gt; string list -&gt; string list
</programlisting>
<para>
Determines the list of direct or indirect ancestors of the packages in
<replaceable>pkglist</replaceable> for the set of
<replaceable>predicates</replaceable>. The returned list is
topologically sorted.
The exception Not_found is raised if the package could
not be found. The exception Failure is raised if one of the ancestors
could not be found, or if a circular dependency has been
detected. Other exceptions may occur as file I/O is done.
</para>
</refsect2>



<refsect2>
<title><anchor id="Findlib.default-location">
Findlib.default_location ()</title>
<programlisting id="val.Findlib.default-location">
val default_location : unit -&gt; string
</programlisting>
<para>
Gets the default location where new packages will be installed.
</para>
</refsect2>

</refsect1>

</refentry>

