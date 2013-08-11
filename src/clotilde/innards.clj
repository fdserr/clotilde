(ns clotilde.innards
  (:use [matchure :only (when-match)]))

(declare -init-local -space -waitq -rdin -out)

(def -space (ref (vector)))

(def -waitq (ref (vector)))

(defn -init-local
  [] 
  (dosync
    (ref-set -waitq (vector))
    (ref-set -space (vector))
    nil))

(defn remove-once
  [coll item]
  (let [[n m] (split-with (partial not= item) coll)] 
    (concat n (rest m))))

(defn -resolve-match-promise
  "Evaluates to: the (potentially resolved) match-promise argument.
  Side effect(s): the promise is delivered if the match succeeds;
  the matched expression is put back in space if mode is :rd."
  [expr match-promise]
  (let [[p f m] match-promise
        e (when-not (realized? p) (f expr))]
    (when e
      (deliver p e)
      (if (= m :rd)
        (-out e -space -waitq))
      match-promise)))

(defn -match-in-waitq
  "Evaluates to: a resolved match-promise against expr from waitq, or nil "
  [expr waitq]
  (first (filter (partial -resolve-match-promise expr) waitq)))

(defn -match-in-space
  "Evaluates to: a pattern-matched expression from space using match-fn, or nil."
  [match-fn space]
  (first (filter match-fn space)))

(defmacro -unquote-pattern
  "TODO: Ugly hack, help needed."
  [pattern]
  `~pattern)

(defn -match-fn
  "Evaluates to: a one argument pattern-matching function. 
  pattern: to match against the fn argument. See org.clojure/core/match.
  Convention: a match function evaluates to the matched expression, or nil."
  [pattern]
  #(when-match [pattern %] %))

(defn- match-tuple
  "TODO"
  [tuple & ptns]
  (when (= (count tuple) (count ptns)) 
    :ok))

(defn -out 
  "out! op implementation.
  Match in queue or add to space."
  [expr space-ref waitq-ref]
  (dosync
    (let [p (-match-in-waitq expr @waitq-ref)]
      (if p
        (alter waitq-ref remove-once p)
        (alter space-ref conj expr)))
    nil))

(defn -rdin
  "rd! and in! ops implementation.
  Match in space or add to queue [promise matcher mode]."
  [pattern mode space-ref waitq-ref]
  (deref 
    (dosync 
      (let [f (-match-fn pattern)
            x (-match-in-space f @space-ref)]
        (if x
          (do
            (when (= mode :in)
              (alter space-ref remove-once x))        
            (delay x))
          (let [p (promise)] 
            (alter waitq-ref conj [p f mode])
            p))))))




