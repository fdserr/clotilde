(ns clotilde.core
  (:use clotilde.innards
        [matchure :only (fn-match)]))

(defn initialize!
  "Evaluates to nil.
  vectors: a vector of vectors.
  Side-effect(s): no arg -> empty space; else space starts with vectors"
  ([] (init-local))
  ([vectors] (init-local vectors)))

(defmacro out!
  "exprs: one or more s-expressions; they'll be evaluated within the calling thread 
  (side effects ok, transactions strictly verboten).
  Evaluates to a tuple form [expr1-result expr2-result .. exprN-result].
  Side-effect(s): some waiting in! or rd! succeed, or the tuple is put in space.
  => (out! :t (+ 1 0) \"One\")
  [:t 1 \"One\"] ;; [:t 1 \"One\"] added to space."
  [& exprs]
  `(out-eval ~@exprs))

(defmacro eval!
  "Just like out!, but exprs are evaluated within a (single) new thread of execution.
  Evaluates to a future; when dereferenced, blocks until fully evaluated, then
  yields a tuple form [expr1-result expr2-result .. exprN-result];
  result is cached (not re-evaluated) on subsequent derefs.
  => (let [f (eval! :t (+ 1 0) (out! :i 1))] ;; [:t 1 [:i 1]] and [:i 1] adding to space...
       @f)
  [:t 1 [:i 1]] ;; return when f is realized (block until then)
  => (eval! :job (Thread/sleep 5000) (+ 1 0)) ;; return fast  
  #<core$future_call$reify__6267@66265039: :pending> ;; [:job nil 1] added to space in ~5 seconds"
  [& exprs]
  `(future (out-eval ~@exprs)))

(defmacro rd! 
  "patterns: a vector of pattern elements to match against tuples in space.
  Valid patterns are literals (eg.: 0, \"bug\", :x, ...), bindings (eg.: x, y, whatnot, ...), 
  wildcards (_ and ?), regexps (eg.: #\"hello\"), and/or variables to be bound within the 
  lexical context of rd! (eg.: ?var, [?fst & ?rst], ...). 
  See matchure on GitHub for more pattern-matching sweetness. 
  Mucho thankies for writing matchure, Drew!
  body: one or more s-expressions to evaluate within the lexical context of rd!.
  Evaluates to body, in an implicit do.
  Side-effect(s): rd! will block until a matching tuple is found (no order assumed in space);
  variable patterns (eg. ?var) are bound to their respective matching value from the tuple,
  within the context of rd! (the pars around it, as in let)."
  [patterns & body]
  (assert (vector? patterns) "Invalid patterns argument given to rd! (must be a vector).")
  `(rd (fn-match 
         ([~patterns] (do ~@body))
         ([~'_] nil))))

#_(defn in!
  "Just like rd!, but the matching tuple is removed from space."
  [patterns & body] 
  (io! @(rd-in 'patterns :in -space -waitq 'body)
       
       
       ))

