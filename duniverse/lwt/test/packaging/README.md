# Packaging tests

This directory contains a few tiny programs that *depend* on Lwt, through one of
two build systems: Findlib or Dune. Each program is built to ensure that Lwt
packaging remains usable by Lwt users. To do this, run `make packaging-test`
from the Lwt repository root. Before that, you may want to run
`make install-for-packaging-test`. This will install Lwt and all helper
libraries into your current OPAM switch. The packaging tests are run against
this installation.
