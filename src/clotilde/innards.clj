(ns clotilde.innards
  (:use [matchure :only (fn-match)]))

(declare -init-local -space -waitq rd-in out-eval)

(def -space (ref (vector)))
(def -waitq-ins (ref (vector)))
(def -waitq-rds (ref (vector)))

(defn -init-local
  [] 
  (dosync
    (ref-set -waitq-ins (vector))
    (ref-set -waitq-rds (vector))
    (ref-set -space (vector))
    nil))

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

(defn -resolve-match-promise
  "Evaluates to: the resolved match-promise argument, or nil.
  Side effect(s): the promise is delivered if the match succeeds."
  [tuple match-promise]
  (let [[p f _] match-promise
        e (f tuple)]
    (when e
      (deliver p e)
      match-promise)))

(defn -match-in-waitq
  "Evaluates to a [promise, index] form where:
  promise: a realized promise referencing a successful match of the tuple arg in waitq, or nil;
  index: index of the resolved match-promise in waitq, or -1."
  [tuple waitq]
  (for [x waitq
        i (.indexOf x)
        mp (-resolve-match-promise tuple x)]
    (when mp
      (let [[_ _ m] mp]
        (when (= :rd m)
          (-match-in-waitq tuple (dissoc waitq i)))))))

(defn out-eval 
  "out! and eval! ops implementation.
  Match in queue or add to space."
  [& exprs]
  (io! (dosync
    (let [t (vec exprs)]
      (when-not nil #_(-match-in-waitq t @-waitq)        
        (commute -space conj t))
      t))))

(defn -match-in-space
  "Evaluates to a [tuple, index] form where:
  tuple: a successful match of match-fn in space, or nil;
  index: index of the succesfully matched tuple in space, or -1."
  [match-fn space]
  (for [x space
        i (.indexOf x)
        e (match-fn x)]
    (if e [e i] [nil -1])))

#_(defn rd-in
  "rd! and in! ops implementation.
  Match in space or add to queue [promise matcher mode]."
  [patterns mode & body]
    (dosync 
      (let [f# (match-fn patterns body)
            [e# i#] (-match-in-space f# @space-ref)]
        #_(if e#
          (do
            (when (= mode :in)
              (alter space-ref dissoc i#))        
            (delay e#))
          (let [p# (promise)] 
            (alter waitq-ref conj [p# f# mode])
            p#)))))




