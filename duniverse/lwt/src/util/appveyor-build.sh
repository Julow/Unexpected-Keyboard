set -e
set -x

eval `opam config env`

date

if [ -d _cache/_build ]
then
    cp -r _cache/_build .
fi

make build

date

dune runtest --force

date

if [ ! -d _cache/_build ]
then
    mkdir -p _cache
    cp -r _build _cache
fi

date
