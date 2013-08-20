(ns clotilde.tools
  (:use clojure.test
        clotilde.innards
        [matchure :only (fn-match)]))

;; Toolset ===========================

(defn empty-space?
  "True if space contains no tuple. Waiting in! or rd! may exist."
  []
  (= 0 (count @-space)))

(defn void-space?
  "True if space contains no tuple and no waiting in! or rd!."
  []
  (= 0 (dosync (+ (count @-space) 
                  (count @-waitq-ins) 
                  (count @-waitq-rds)))))

(defn print-space
  "Guess what."
  []
  (let [s (dosync (str "================================" "\n"
                       (apply str (interpose "\n" (map str (sort @-space))))
                       "\n" "================================" "\n"
                       "Tuples in space: " (count @-space)
                       "; waiting rd!: " (count @-waitq-rds)
                       "; waiting in!: " (count @-waitq-ins)))]
    (println s)))


