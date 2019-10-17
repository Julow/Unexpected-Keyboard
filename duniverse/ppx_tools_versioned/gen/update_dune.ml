let versions = ["402"; "403"; "404"; "405"; "406"; "407"; "408"; "409"]

let flags = "(:standard -w +A-4-17-44-45-105-42 -safe-string)"

let ppx_tools_versioned_modules =
  versions
  |> List.map (fun v ->
      [ "ast_convenience"
      ; "ast_mapper_class"
      ; "ast_lifter"
      ; "ppx_tools" ]
      |> List.map (fun m -> Printf.sprintf "%s_%s" m v)
      |> String.concat " "
    )
  |> String.concat "\n"

let () =
  Printf.printf
    {sexp|
(library
 (name ppx_tools_versioned)
 (public_name ppx_tools_versioned)
 (synopsis "Tools for authors of ppx rewriters and other syntactic tools (with ocaml-migrate-parsetree support)")
 (libraries ocaml-migrate-parsetree)
 (flags %s)
 (wrapped false)
 (modules %s))
|sexp} flags ppx_tools_versioned_modules

let synopsis_v v =
  Printf.sprintf "%c.%s" v.[0] (String.sub v 1 (String.length v - 1))

let () =
  List.iter (fun version ->
      Printf.printf
        {sexp|
(library
 (name ppx_tools_versioned_metaquot_%s)
 (public_name ppx_tools_versioned.metaquot_%s)
 (synopsis "Meta-quotation: %s parsetree quotation")
 (libraries ocaml-migrate-parsetree ppx_tools_versioned)
 (kind ppx_rewriter)
 (wrapped false)
 (modules ppx_metaquot_%s)
 (flags %s))
|sexp} version version (synopsis_v version) version flags
    ) versions
