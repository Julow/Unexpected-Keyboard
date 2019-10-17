<!DOCTYPE style-sheet PUBLIC "-//James Clark//DTD DSSSL Style Sheet//EN" [
<!ENTITY dbstyle SYSTEM "docbook.dsl" CDATA DSSSL>
]>

<style-sheet>
<style-specification use="docbook">
<style-specification-body>

(define %footnotes-at-end%
  ;; Should footnotes appear at the end of HTML pages?
  #t)

(define %html-ext% 
  ;; Default extension for HTML output files
  ".html")

(define %root-filename%
  ;; Name for the root HTML document
  "index")

</style-specification-body>
</style-specification>
<external-specification id="docbook" document="dbstyle">
</style-sheet>
