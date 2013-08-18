(ns clotilde.innards
  (:use [matchure :only (fn-match)]))

;; Nothing private => easy test and easy plugs

(defonce 
  ^{:doc "Tuple space."} 
  -space (ref (vector)))

(defonce 
  ^{:doc "in! ops wait queue."} 
  -waitq-ins (ref (vector)))

(defonce 
  ^{:doc "rd! ops wait queue."} 
  -waitq-rds (ref (vector)))

(defn init-local
  "Start a fresh space, blank or equal to vectors."
  ([] 
    (dosync
      (ref-set -waitq-ins (vector))
      (ref-set -waitq-rds (vector))
      (ref-set -space (vector))
      nil))
  ([vectors] 
    (assert (vector? vectors) 
            "illegal argument given to initialize!; vectors must be a vector.")
    (assert (every? vector? vectors) 
            "illegal argument given to initialize!; vectors can only contain vectors.")
    (assert (not (some empty? vectors)) 
            "illegal argument given to initialize!; vectors cannot contain empty vectors.")
    (dosync
      (ref-set -waitq-ins (vector))
      (ref-set -waitq-rds (vector))
      (ref-set -space (vec vectors))
      nil)))

(defn extract-all-with
  "=> (extract-all-with [1 2 3 4 5 6] #(when (odd? %) (* 2 %)))
  [[2 6 10] [2 4 6]]"
  ([coll fun]
    (extract-all-with coll fun (transient []) (transient [])))
  ([coll fun applied not-applied]
    (if-not (seq coll)
      [(persistent! applied) (persistent! not-applied)]
      (if-let 
        [x (fun (first coll))]
        (recur (rest coll) fun (conj! applied x) not-applied)
        (recur (rest coll) fun applied (conj! not-applied (first coll)))))))

(defn extract-one-with
  "=> (extract-one-with [12 2 3 4 5 6] #(when (odd? %) (* 2 %)))
  [[6] [12 2 4 5 6]]"
  ([coll fun]
    (extract-one-with coll fun [] []))
  ([coll fun applied not-applied]
    (if-not (seq coll)
      [applied not-applied]
      (if-let 
        [x (fun (first coll))]
        (recur (empty coll) fun (conj applied x) (vec (concat not-applied (rest coll))))
        (recur (rest coll) fun applied (conj not-applied (first coll)))))))

(defn resolve-match-promise?
  "Attempt to resolve a match promise.
  Eval to matcher result or nil."
  [tuple [promise matcher]]
  (when-let [e (matcher tuple)]
    (deliver promise e)))

(defn match-in-queue?
  "Pass tuple to all waiting rd!s and to some in!s.
  Return truthy as soon as an in! matches, else eval to nil."
  [tuple]
  (let [rslv (partial resolve-match-promise? tuple)
        [rd-ok rd-nok] (extract-all-with @-waitq-rds rslv)
        [in-ok in-nok] (extract-one-with @-waitq-ins rslv)]
    (when-not (empty? rd-ok)
      (ref-set -waitq-rds rd-nok))
    (when-not (empty? in-ok)
      (ref-set -waitq-ins in-nok))
    (not-empty in-ok)))

(defn out-eval 
  "out! and eval! ops implementation.
  Match in Qs or add to space."
  [& exprs]
  (io! (dosync
         (let [t (vec exprs)]
           (when-not (match-in-queue? t)        
             (alter -space conj t))
           t))))

(defn rd
  "rd! op implementation.
  Match in space or Q up."
  [matcher]
  (io! 
    (deref 
      (dosync
        (ensure -waitq-ins)
        (let [e (->> @-space (map matcher) (drop-while nil?) first)
              p (if e (delay e) (promise))]
          (when-not e
            (alter -waitq-rds conj [p matcher]))
          p)))))

(defn in
  "in! op implementation.
  Remove match from space or Q up."
  [matcher]
  (io!
    (deref
      (dosync
        (ensure -waitq-rds)
        (let [[ok nok] (extract-one-with @-space matcher)
              e (first ok)
              p (if e (delay e) (promise))]
          (if-not (empty? ok)
            (ref-set -space nok)
            (alter -waitq-ins conj [p matcher]))
          p)))))

