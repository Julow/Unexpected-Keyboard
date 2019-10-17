(* $Id$
 * ----------------------------------------------------------------------
 *
 *)


let in_words s =
  (* splits s in words separated by commas and/or whitespace *)
  let l = String.length s in
  let rec split i j =
    if j < l then
      match s.[j] with
	(' '|'\t'|'\n'|'\r'|',') ->
	  if i<j then (String.sub s i (j-i)) :: (split (j+1) (j+1))
		 else split (j+1) (j+1)
      |	_ ->
	  split i (j+1)
    else
      if i<j then [ String.sub s i (j-i) ] else []
  in
  split 0 0
;;


let in_words_ws s =
  (* splits s in words separated by whitespace *)
  let l = String.length s in
  let rec split i j =
    if j < l then
      match s.[j] with
	(' '|'\t'|'\n'|'\r') ->
	  if i<j then (String.sub s i (j-i)) :: (split (j+1) (j+1))
		 else split (j+1) (j+1)
      |	_ ->
	  split i (j+1)
    else
      if i<j then [ String.sub s i (j-i) ] else []
  in
  split 0 0
;;


let package_name s =
  (* splits s in words separated by dots.
   * As a special case, when s="." the package "." is returned.
   *)
  let l = String.length s in
  let rec split i j =
    if j < l then
      match s.[j] with
	'.' ->
	  if i<j then (String.sub s i (j-i)) :: (split (j+1) (j+1))
		 else split (j+1) (j+1)
      |	_ ->
	  split i (j+1)
    else
      if i<j then [ String.sub s i (j-i) ] else []
  in
  if s="." then
    ["."]
  else
    split 0 0
;;


let is_valid_package_name s =
  (* Use this only for installation/deinstallation of packages! *)
  not(String.contains s '.')
;;


let path_separator =
  match Sys.os_type with
      "Unix"   -> ':'
    | "Cygwin" -> ';'   (* You might want to change this *)
    | "Win32"  -> ';'
    | "MacOS"  -> failwith "Findlib: I do not know what is the correct path separator for MacOS. If you can help me, write a mail to gerd@gerd-stolpmann.de"
    | _ -> failwith "Findlib: unknown operating system"
;;


let path str =
  (* split "str" into parts separated by "path_separator" *)
  let l = String.length str in
  let rec split_up j k =
    if k < l then begin
      let c = str.[k] in
      if c = path_separator then begin
        if k - j > 0 then
	  String.sub str j (k-j) :: split_up (k+1) (k+1)
        else
	  split_up (k+1) (k+1)
      end
      else
	split_up j (k+1)
    end
    else
      if k - j > 0 then
        [ String.sub str j (k-j) ]
      else
	[]
  in
  split_up 0 0
;;


let norm_dir s =
  (* Converts the file name of the directory [d] to the normal form.
   * For Unix, the '/' characters at the end are removed, and multiple
   * '/' are deleted.
   * For Windows, all '/' characters are converted to '\'. Two
   * backslashes at the beginning are tolerated.
   *)
  let b = Buffer.create 80 in
  let l = String.length s in
  let norm_dir_unix() =
    Buffer.add_char b s.[0];
    for k = 1 to l - 1 do
      let c = s.[k] in
      if not ((c = '/' && s.[k-1] = '/') || (c = '/' && k = l-1)) then
        Buffer.add_char b c
    done
  in
  let is_slash =
    function
    | '/' | '\\' -> true
    | _ -> false in
  let norm_dir_win() =
    if l >= 1 && s.[0] = '/' then
      Buffer.add_char b '\\' else Buffer.add_char b s.[0];
    if l >= 2 && s.[1] = '/' then
      Buffer.add_char b '\\' else Buffer.add_char b s.[1];
    for k = 2 to l - 1 do
      let c = s.[k] in
      if is_slash c then (
        if not (is_slash s.[k-1] || k = l-1) then
          Buffer.add_char b '\\'
      ) else 
        Buffer.add_char b c
    done
  in
  match Sys.os_type with
      "Unix" | "Cygwin" -> norm_dir_unix(); Buffer.contents b
    | "Win32" -> norm_dir_win(); Buffer.contents b
    | _ -> failwith "This os_type is not supported"
;;
