
(library
 (name ppx_tools_versioned)
 (public_name ppx_tools_versioned)
 (synopsis "Tools for authors of ppx rewriters and other syntactic tools (with ocaml-migrate-parsetree support)")
 (libraries ocaml-migrate-parsetree)
 (flags (:standard -w +A-4-17-44-45-105-42 -safe-string))
 (wrapped false)
 (modules ast_convenience_402 ast_mapper_class_402 ast_lifter_402 ppx_tools_402
ast_convenience_403 ast_mapper_class_403 ast_lifter_403 ppx_tools_403
ast_convenience_404 ast_mapper_class_404 ast_lifter_404 ppx_tools_404
ast_convenience_405 ast_mapper_class_405 ast_lifter_405 ppx_tools_405
ast_convenience_406 ast_mapper_class_406 ast_lifter_406 ppx_tools_406
ast_convenience_407 ast_mapper_class_407 ast_lifter_407 ppx_tools_407
ast_convenience_408 ast_mapper_class_408 ast_lifter_408 ppx_tools_408
ast_convenience_409 ast_mapper_class_409 ast_lifter_409 ppx_tools_409))

(library
 (name ppx_tools_versioned_metaquot_402)
 (public_name ppx_tools_versioned.metaquot_402)
 (synopsis "Meta-quotation: 4.02 parsetree quotation")
 (libraries ocaml-migrate-parsetree ppx_tools_versioned)
 (kind ppx_rewriter)
 (wrapped false)
 (modules ppx_metaquot_402)
 (flags (:standard -w +A-4-17-44-45-105-42 -safe-string)))

(library
 (name ppx_tools_versioned_metaquot_403)
 (public_name ppx_tools_versioned.metaquot_403)
 (synopsis "Meta-quotation: 4.03 parsetree quotation")
 (libraries ocaml-migrate-parsetree ppx_tools_versioned)
 (kind ppx_rewriter)
 (wrapped false)
 (modules ppx_metaquot_403)
 (flags (:standard -w +A-4-17-44-45-105-42 -safe-string)))

(library
 (name ppx_tools_versioned_metaquot_404)
 (public_name ppx_tools_versioned.metaquot_404)
 (synopsis "Meta-quotation: 4.04 parsetree quotation")
 (libraries ocaml-migrate-parsetree ppx_tools_versioned)
 (kind ppx_rewriter)
 (wrapped false)
 (modules ppx_metaquot_404)
 (flags (:standard -w +A-4-17-44-45-105-42 -safe-string)))

(library
 (name ppx_tools_versioned_metaquot_405)
 (public_name ppx_tools_versioned.metaquot_405)
 (synopsis "Meta-quotation: 4.05 parsetree quotation")
 (libraries ocaml-migrate-parsetree ppx_tools_versioned)
 (kind ppx_rewriter)
 (wrapped false)
 (modules ppx_metaquot_405)
 (flags (:standard -w +A-4-17-44-45-105-42 -safe-string)))

(library
 (name ppx_tools_versioned_metaquot_406)
 (public_name ppx_tools_versioned.metaquot_406)
 (synopsis "Meta-quotation: 4.06 parsetree quotation")
 (libraries ocaml-migrate-parsetree ppx_tools_versioned)
 (kind ppx_rewriter)
 (wrapped false)
 (modules ppx_metaquot_406)
 (flags (:standard -w +A-4-17-44-45-105-42 -safe-string)))

(library
 (name ppx_tools_versioned_metaquot_407)
 (public_name ppx_tools_versioned.metaquot_407)
 (synopsis "Meta-quotation: 4.07 parsetree quotation")
 (libraries ocaml-migrate-parsetree ppx_tools_versioned)
 (kind ppx_rewriter)
 (wrapped false)
 (modules ppx_metaquot_407)
 (flags (:standard -w +A-4-17-44-45-105-42 -safe-string)))

(library
 (name ppx_tools_versioned_metaquot_408)
 (public_name ppx_tools_versioned.metaquot_408)
 (synopsis "Meta-quotation: 4.08 parsetree quotation")
 (libraries ocaml-migrate-parsetree ppx_tools_versioned)
 (kind ppx_rewriter)
 (wrapped false)
 (modules ppx_metaquot_408)
 (flags (:standard -w +A-4-17-44-45-105-42 -safe-string)))

(library
 (name ppx_tools_versioned_metaquot_409)
 (public_name ppx_tools_versioned.metaquot_409)
 (synopsis "Meta-quotation: 4.09 parsetree quotation")
 (libraries ocaml-migrate-parsetree ppx_tools_versioned)
 (kind ppx_rewriter)
 (wrapped false)
 (modules ppx_metaquot_409)
 (flags (:standard -w +A-4-17-44-45-105-42 -safe-string)))
