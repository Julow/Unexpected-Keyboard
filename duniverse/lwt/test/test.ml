(* This file is part of Lwt, released under the MIT license. See LICENSE.md for
   details, or visit https://github.com/ocsigen/lwt/blob/master/LICENSE.md. *)



type test = {
  test_name : string;
  skip_if_this_is_false : unit -> bool;
  sequential : bool;
  run : unit -> bool Lwt.t;
}

type outcome =
  | Passed
  | Failed
  | Exception of exn
  | Skipped

exception Skip
exception Duplicate_Test_Names of string

let test_direct test_name ?(only_if = fun () -> true) run =
  let run =
    fun () ->
      Lwt.return (run ())
  in
  {test_name; skip_if_this_is_false = only_if; sequential = false; run}

let test test_name ?(only_if = fun () -> true) ?(sequential = false) run =
  {test_name; skip_if_this_is_false = only_if; sequential; run}

module Log =
struct
  let log_file =
    let pid = Unix.getpid () in
    let ms = Unix.gettimeofday () |> modf |> fst in
    let filename = Printf.sprintf "test.%i.%03.0f.log" pid (ms *. 1e3) in
    open_out filename
  let () =
    at_exit (fun () -> close_out_noerr log_file)

  let start_time = ref None
  let elapsed () =
    let now = Unix.gettimeofday () in
    match !start_time with
    | None ->
      start_time := Some now;
      0.
    | Some start_time ->
      now -. start_time

  let write identifier message =
    Printf.ksprintf (output_string log_file) "%s [%07.3f]: %s\n"
      identifier (mod_float (elapsed ()) 1000.) message;
    flush log_file
  let log k =
    k (fun identifier ->
      Printf.ksprintf (write identifier))
end

let log = Log.log

let run_test : test -> outcome Lwt.t = fun test ->
  if test.skip_if_this_is_false () = false then begin
    log @@ (fun k -> k test.test_name "skipping");
    Lwt.return Skipped
  end

  else begin
    let start_time = Unix.gettimeofday () in
    log @@ (fun k -> k test.test_name "starting");

    (* Lwt.async_exception_hook handling inspired by
         https://github.com/mirage/alcotest/issues/45 *)
    let async_exception_promise, async_exception_occurred = Lwt.task () in
    let old_async_exception_hook = !Lwt.async_exception_hook in
    Lwt.async_exception_hook := (fun exn ->
      Lwt.wakeup_later async_exception_occurred (Exception exn));

    Lwt.finalize
      (fun () ->
        let test_completion_promise =
          Lwt.try_bind
            (fun () ->
              test.run ())

            (fun test_did_pass ->
              if test_did_pass then
                Lwt.return Passed
              else
                Lwt.return Failed)

            (function
            | Skip ->
              Lwt.return Skipped

            | exn_raised_by_test ->
              Lwt.return (Exception exn_raised_by_test))
        in

        Lwt.pick [test_completion_promise; async_exception_promise])

      (fun () ->
        Lwt.async_exception_hook := old_async_exception_hook;
        let elapsed = Unix.gettimeofday () -. start_time in
        log @@ (fun k -> k test.test_name "finished in %.3f s" elapsed);
        Lwt.return_unit)
  end

let outcome_to_character : outcome -> string = function
  | Passed -> "."
  | Failed -> "F"
  | Exception _ -> "E"
  | Skipped -> "S"



type suite = {
  suite_name : string;
  suite_tests : test list;
  skip_suite_if_this_is_false : unit -> bool;
}

let contains_dup_tests suite tests =
  let names =
    List.map (fun t -> "suite:" ^ suite ^ " test:" ^ t.test_name) tests in
  let sorted_unique_names = List.sort_uniq String.compare names in
  let counts =
    List.map (fun x ->
      let tests = List.find_all (fun y -> y = x) names in
      (x, List.length tests)) sorted_unique_names in
  let dups = List.filter (fun (_, count) -> count > 1) counts |>
                          List.map (fun (name, _) -> name) in
  if List.length dups > 0 then
    Some dups
  else
    None

let suite name ?(only_if = fun () -> true) tests =
  match contains_dup_tests name tests with
    | Some names -> raise (Duplicate_Test_Names (String.concat ", " names))
    | None -> ();
  {suite_name = name;
   suite_tests = tests;
   skip_suite_if_this_is_false = only_if}

let run_test_suite : suite -> ((string * outcome) list) Lwt.t = fun suite ->
  if suite.skip_suite_if_this_is_false () = false then
    let outcomes =
      suite.suite_tests
      |> List.map (fun {test_name; _} -> (test_name, Skipped))
    in
    (outcome_to_character Skipped).[0]
    |> String.make (List.length outcomes)
    |> print_string;
    flush stdout;

    Lwt.return outcomes

  else
    suite.suite_tests |> Lwt_list.map_s begin fun test ->
      Lwt.bind (run_test test) (fun outcome ->
      outcome |> outcome_to_character |> print_string;
      flush stdout;
      Lwt.return (test.test_name, outcome))
    end

let outcomes_all_ok : (_ * outcome) list -> bool = fun outcomes ->
  outcomes
  |> List.for_all (fun (_test_name, outcome) ->
    match outcome with
    | Passed | Skipped -> true
    | Failed | Exception _ -> false)

let show_failures : (string * outcome) list -> unit =
  List.iter (fun (test_name, outcome) ->
    match outcome with
    | Passed
    | Skipped ->
      ()

    | Failed ->
      Printf.eprintf
        "Test '%s' produced 'false'\n" test_name

    | Exception exn ->
      Printf.eprintf
        "Test '%s' raised '%s'\n" test_name (Printexc.to_string exn))



type ('a, 'b) aggregated_outcomes = ('a * (('b * outcome) list)) list

let fold_over_outcomes :
    ('a -> outcome -> 'a) ->
    'a ->
    (_, _) aggregated_outcomes ->
      'a =
    fun f init outcomes ->

  List.fold_left (fun accumulator (_suite_name, test_outcomes) ->
    List.fold_left (fun accumulator (_test_name, test_outcome) ->
      f accumulator test_outcome)
      accumulator
      test_outcomes)
    init
    outcomes

let count_ran : (_, _) aggregated_outcomes -> int = fun outcomes ->
  outcomes
  |> fold_over_outcomes
    (fun count -> function
      | Skipped ->
        count
      | _ ->
        count + 1)
    0

let count_skipped : (_, _) aggregated_outcomes -> int = fun outcomes ->
  outcomes
  |> fold_over_outcomes
    (fun count -> function
      | Skipped ->
        count + 1
      | _ ->
        count)
    0

(* Runs a series of test suites. If one of the test suites fails, does not run
   subsequent suites. *)
let run library_name suites =
  Printexc.register_printer (function
    | Failure message -> Some (Printf.sprintf "Failure(%S)" message)
    | _ -> None);

  Printf.printf "Testing library '%s'...\n" library_name;

  let start_time = Unix.gettimeofday () in

  let rec loop_over_suites aggregated_outcomes suites =
    match suites with
    | [] ->
      let end_time = Unix.gettimeofday () in
      Printf.printf
        "\nOk. %i tests ran, %i tests skipped in %.2f seconds\n"
        (count_ran aggregated_outcomes)
        (count_skipped aggregated_outcomes)
        (end_time -. start_time);
      Lwt.return_unit

    | suite::rest ->
      Lwt.bind (run_test_suite suite) begin fun outcomes ->
        if not (outcomes_all_ok outcomes) then begin
          print_newline ();
          flush stdout;
          Printf.eprintf "Failures in test suite '%s':\n" suite.suite_name;
          show_failures outcomes;
          exit 1
        end
        else
          loop_over_suites
            ((suite.suite_name, outcomes)::aggregated_outcomes) rest
      end
  in

  loop_over_suites [] suites
  |> Lwt_main.run

let concurrent library_name suites =
  Printexc.register_printer (function
    | Failure message -> Some (Printf.sprintf "Failure(%S)" message)
    | _ -> None);

  Printf.printf "Testing library '%s'...\n" library_name;

  let open Lwt.Infix in

  let run_test (suite, test) =
    begin
      if suite.skip_suite_if_this_is_false () = false then
        Lwt.return Skipped
      else
        run_test test
    end
    >|= fun outcome ->
    print_string (outcome_to_character outcome);
    flush stdout;
    ((suite, test), outcome)
  in

  let start_time = Unix.gettimeofday () in

  (* List all the tests. *)
  suites
  |> List.map (fun suite ->
    suite.suite_tests
    |> List.map (fun test ->
      (suite, test)))
  |> List.flatten

  (* Separate the tests that must be run sequentially, and run them. *)
  |> List.partition (fun (_suite, test) -> test.sequential)
  |> fun (sequential, concurrent) ->
    Lwt_list.map_s run_test sequential
  >>= fun sequential_outcomes ->

  (* Run the tests that can be run concurrently. *)
  concurrent
  |> Lwt_list.map_p run_test

  (* Summarize the results. *)
  >>= fun concurrent_outcomes ->
  let outcomes = sequential_outcomes @ concurrent_outcomes in
  if outcomes_all_ok outcomes then
    let end_time = Unix.gettimeofday () in
    let aggregated_outcomes = [(), outcomes] in
    Printf.printf
      "\nOk. %i tests ran, %i tests skipped in %.2f seconds\n"
      (count_ran aggregated_outcomes)
      (count_skipped aggregated_outcomes)
      (end_time -. start_time);
    Lwt.return_unit
  else begin
    print_newline ();
    flush stdout;
    outcomes |> List.iter (function
      | (suite, test), Failed ->
        Printf.eprintf "Test '%s' in suite '%s' produced 'false'\n"
          test.test_name suite.suite_name
      | (suite, test), Exception exn ->
        Printf.eprintf "Test '%s' in suite '%s' raised '%s'\n"
          test.test_name suite.suite_name (Printexc.to_string exn)
      | _ ->
        ());
    exit 1
  end

let concurrent library_name suites =
  Lwt_main.run (concurrent library_name suites)

let with_async_exception_hook hook f =
  let old_hook = !Lwt.async_exception_hook in
  Lwt.async_exception_hook := hook;
  Lwt.finalize f (fun () ->
    Lwt.async_exception_hook := old_hook;
    Lwt.return ())
