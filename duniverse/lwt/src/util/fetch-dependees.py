#! /usr/bin/env python

# Retrieves source code of OPAM packages recursively depending on Lwt into a
# subdirectory ./dependees/, so you can grep through the code.

import os.path
import subprocess
import time

DEPENDEES = "dependees"

def main():
    now = time.time()

    subprocess.check_call(["opam", "update"])

    packages = subprocess.check_output([
        "opam", "list", "--all", "--depends-on=lwt", "--dev", "--recursive",
        "--short"])

    depopt_packages = subprocess.check_output([
        "opam", "list", "--all", "--depends-on=lwt", "--depopts", "--dev",
        "--short", "--with-test", "--with-doc"])

    packages = packages.strip().split("\n")
    depopt_packages = depopt_packages.strip().split("\n")

    packages = set(packages).union(set(depopt_packages))
    packages = list(packages)
    packages.sort()

    print "Downloading %i packages..." % len(packages)

    for package in packages:
        directory = os.path.join(DEPENDEES, package)

        try:
            timestamp = os.path.getmtime(directory)
            if now - timestamp < 24 * 60 * 60:
                continue
        except:
            pass
        remove_command = ["rm", "-rf", directory]
        subprocess.check_call(remove_command)

        use_opam_source = False

        info_command = ["opam", "info", "--field=dev-repo:", package]
        try:
            field = subprocess.check_output(info_command)
            no_quotes = field[1:-2]
            if no_quotes[0:9] == "git+https":
                repo = no_quotes[4:]
            elif no_quotes[0:16] == "git://github.com":
                if no_quotes[-1:] == "/":
                    no_quotes = no_quotes[:-1]
                repo = "https" + no_quotes[3:]
            else:
                use_opam_source = True

            if use_opam_source == False:
                clone_command = \
                    ["git", "clone", "-q", "--depth", "1", repo, directory]
                print "[%s]" % package, " ".join(clone_command)
                subprocess.check_call(clone_command)
        except:
            use_opam_source = True

        if not use_opam_source:
            continue

        source_command = ["opam", "source", "--dir=" + directory]
        try:
            subprocess.check_call(source_command + ["--dev-repo", package])
        except subprocess.CalledProcessError as e:
            subprocess.check_call(remove_command)
            try:
                subprocess.check_call(source_command + [package])
            except subprocess.CalledProcessError as e:
                pass

if __name__ == "__main__":
    main()
