<refentry>

<refmeta>
  <refentrytitle>site-lib</refentrytitle>
  <manvolnum>5</manvolnum>
  <refmiscinfo>The findlib package manager for OCaml</refmiscinfo>
</refmeta>

<refnamediv id="site-lib">
  <refname>site-lib</refname>
  <refpurpose>[Location of package directories]</refpurpose>
</refnamediv>


<refsynopsisdiv>
<title>STANDARD LAYOUT</title>
<synopsis>
...somewhere in the filesystem hierarchy...
   |
   \ 
     site-lib
     |
     +- (optional) stublibs
     +- (optional) postinstall
     +- (optional) postremove
     |
     +- <replaceable>package1</replaceable>
     |  |
     |  +- META
     |  +- <replaceable>archive files</replaceable>
     |  +- <replaceable>interface definitions</replaceable>
     |
     +- <replaceable>package2</replaceable>
     +
     :
     :
     \
        <replaceable>packageN</replaceable>
</synopsis>
</refsynopsisdiv>

<refsect1>
<title>DESCRIPTION</title>
<para>
Every installation of "findlib" has a default location for package
directories, which is normally a directory called "site-lib". The
location can be set by the configuration variables
<literal>path</literal> (used to look up packages), and
<literal>destdir</literal> (used to install new packages);
see <link linkend="findlib.conf">findlib.conf</link>.
</para>

<para>
The name of a package is the name of the package directory. For
example, if <literal>destdir=/usr/local/lib/ocaml/site-lib</literal>, the
package p will be installed in the subdirectory
<literal>/usr/local/lib/ocaml/site-lib/p</literal>. This subdirectory
must contain the META file and all other files belonging to the package.
Package names must not contain the '.' character.
</para>

<para>
The variable <literal>destdir</literal> specifies the directory for
new packages. You can only have one such directory at a time; but of
course you can change this directory in findlib.conf. The command
<literal>ocamlfind install</literal> puts new packages into this
directory; it is recommended to use this command for installation
because it ensures that the directory layout is right.
</para>

<para>
For searching packages, findlib uses (only) the variable
<literal>path</literal> which may name several locations to look at.
</para>

<para>
For systems with DLL support another directory may exist: stublibs. 
If present, findlib will install DLLs into this directory that is
shared by all packages at the same site-lib location. Findlib remembers
which DLL belongs to which package by special files with the suffix
".owner"; e.g. for the DLL "dllpcre.so" there is another file
"dllpcre.so.owner" containing the string "pcre", so findlib knows
that the package "pcre" owns this DLL. It is not possible that a DLL
is owned by several packages.
</para>

<para>
If the stublibs directory does not exist, DLLs are installed regularly
in the package directories like any other file.
</para>

<para>
For special needs, a postinstall and/or a postremove script may be
installed in the site-lib directory. These scripts are invoked after
installation or removal of a package, respectively.
</para>

</refsect1>


<refsect1>
<title>ALTERNATE LAYOUT</title>
<para>

<programlisting>
...somewhere in the filesystem hierarchy...
   |
   \ 
     site-lib
     |
     +- (optional) stublibs
     +- (optional) postinstall
     +- (optional) postremove
     |
     +- <replaceable>package1</replaceable>
     |  |
     |  +- <replaceable>archive files</replaceable>
     |  +- <replaceable>interface definitions</replaceable>
     |
     +- <replaceable>package2</replaceable>
     +
     :
     :
     \
  :     <replaceable>packageN</replaceable>
  |
  \
    metaregistry
    |
    +- META.package1
    +- META.package2
    +
    :
    \
       META.packageN
</programlisting>
</para>

<para>
This is an alternate directory layout collecting all META files in one
directory. You can configure this layout by setting
<literal>path</literal> to the absolute location of
<literal>metaregistry</literal>. Findlib recognizes that there are
META files in this directory and uses them; it is not necessary to
include <literal>site-lib</literal> into the <literal>path</literal>.
</para>

<para>
In order to work, the META files must contain a
<literal>directory</literal> directive pointing to the corresponding
package directory that resides below <literal>site-lib</literal>.
</para>

<para>
The command <literal>ocamlfind install</literal> copes with this
layout, too. The variable <literal>destdir</literal> must contain the
absolute location of <literal>site-lib</literal>, and the variable
<literal>metadir</literal> must contain the absolute location of
<literal>metaregistry</literal>. Note that <literal>ocamlfind
install</literal> automatically adds a <literal>directory</literal>
directive to the META file, so you need not do it manually.
</para>
</refsect1>

</refentry>
