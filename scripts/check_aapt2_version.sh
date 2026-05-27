# See build.gradle.kts

if [[ "$2" = "" ]]; then exit 0; fi

version()
{
  v=$("$1" version 2>&1)
  echo ${v%%-*}
}

sdk_ver=$(version "$1")
override_ver=$(version "$2")

if ! [[ $sdk_ver = $override_ver ]]; then
  echo "aapt2 version mismatch (sdk: $sdk_ver, override: $override_ver)"
  exit 1
fi
