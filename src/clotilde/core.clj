(ns clotilde.core
  (:use clotilde.innards))

(defn initialize!
  "Evaluates to nil.
  Side-effect: empty space."
  []
  (io! (-init-local)) nil)

(defmacro out!
  "exprs: one or more s-expressions; they'll be evaluated within the calling thread 
  (side effects ok, transactions strictly verboten).
  Evaluates to a tuple form [expr1-result expr2-result .. exprN-result].
  Side-effect(s): some waiting in! or rd! succeed, or the tuple is put in space."
  [& exprs]
  `(out-eval ~@exprs))

(defmacro eval!
  "Just like out!, but exprs are evaluated within a (single) new thread of execution.
  Evaluates to a future; 
  when dereferenced, yields a tuple form [expr1-result expr2-result .. exprN-result]
  and blocks until fully evaluated."
  [& exprs]
  `(future (out-eval ~@exprs)))

#_(defn rd! 
  "patterns: a vector of pattern elements to match against tuples in space.
  Valid patterns are literals (eg.: 0, \"bug\", :x, ...), bindings (eg.: x, y, whatnot, ...), 
  wildcards (_ and ?), regexps (eg.: #\"hello\"), and/or variables to be bound within the 
  lexical context of rd! (eg.: ?var, [?fst & ?rst], ...). 
  See matchure on GitHub for much, much more. Mucho thankies for writing matchure, Drew!
  body: one or more s-expressions to evaluate within the lexical context of rd!.
  Evaluates to body, in an implicit do.
  Side-effect(s): rd! will block until a matching tuple is found (no order assumed in space),
  and variables (eg. ?var) are bound to their respective matching value from the tuple."
  [patterns & body] 
  (io! @(rd-in 'patterns :rd -space -waitq 'body)
       
       
       ))

#_(defn in!
  "Just like rd!, but the matching tuple is removed from space."
  [patterns & body] 
  (io! @(rd-in 'patterns :in -space -waitq 'body)
       
       
       ))

