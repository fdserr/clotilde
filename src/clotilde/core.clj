(ns clotilde.core
  (:use clotilde.innards))

(defn initialize!
  "Evaluates to nil.
  Side-effect: empty space."
  []
  (io! (-init-local)))

(defmacro out!
  "exprs: one or more s-expressions (they'll be evaluated in the calling thread, 
  side effects ok, transactions verboten).
  Evaluates to nil.
  Side-effect(s): some blocked in! or rd! may succeed, 
  or a tuple [expr1-result expr2-result .. exprN-result] is put in space."
  [& exprs]
  `(io! (-out (vector ~@exprs) -space -waitq) nil))

(defmacro eval!
  "Just like out!, but exprs are evaluated within a (single) new thread."
  [& exprs]
  `(io! (future (-out (vector ~@exprs) -space -waitq)) nil))

(defmacro rd! 
  "pattern-elements: one ore more pattern elements to match against tuples in space (see matchure).
  Evaluates to nil.
  Side-effect(s): rd! will block until a matching tuple is found (no order assumed in space),
  and variables (eg. ?var) are bound to their respective matching value from the tuple."
  [& pattern-elements] 
  `(io! (-rdin (quote (vector ~@pattern-elements)) :rd -space -waitq)) nil)

(defmacro in!
  "Just like rd!, but the matching tuple is removed from space."
  [& pattern-elements] 
  `(io! (-rdin (quote (vector ~@pattern-elements)) :in -space -waitq)) nil)

