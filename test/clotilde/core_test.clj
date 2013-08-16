(ns clotilde.core-test
  (:use clojure.test
        clotilde.core
        clotilde.innards
        [matchure :only (fn-match)]))

(defmacro safe-ops
  [time-out & exprs]
  `(deref (future ~@exprs) ~time-out nil))

(deftest test-tools
  (testing "safe-ops"
           (is (= nil (safe-ops 10 (Thread/sleep 100) :ok)))
           (is (= :ok (safe-ops 100 (Thread/sleep 10) :ok)))))

(deftest test-initialize!
  (testing "New empty space."
           (initialize!)
           (dosync
             (is (= [] @-space))
             (is (= [] @-waitq-rds))
             (is (= [] @-waitq-ins))))
  (testing "Clearing randomly filled space and queues."
           (dosync
             (commute -space conj [:x :y])
             (commute -waitq-rds conj [(promise), (partial + 1)])
             (commute -waitq-ins conj [(promise), (partial + 1)])))
           (initialize!)
           (dosync 
             (is (= [] @-space))
             (is (= [] @-waitq-rds))
             (is (= [] @-waitq-ins)))
  #_(testing "Clearing space and queue."
           (out! :x 1)
           (future (rd! [:y] (1)))
           (Thread/sleep 100)
           (dosync 
             (is (= 1 (count @-space)) "Space not empty?")
             (is (= 1 (count @-waitq)) "Queue not empty?"))             
           (initialize!)
           (dosync 
             (is (= [] @-space) "Emptied space?")
             (is (= [] @-waitq) "Emptied queue?"))))

(deftest test-out!
  (testing "(out! :x)."
           (initialize!)
           (is (= [:x] (out! :x)))
           (dosync
             (is (= [[:x]] @-space) "Space contains [:x]?")))
  (testing "N-times (out! :x)"
           (initialize!)
           (out! :x)
           (out! :x)
           (dosync
             (is (= [[:x] [:x]] @-space) "Space contains [:x] twice?")))
  (testing "(out! (+ 1 2) (+ 2 3)"
           (initialize!)
           (is (= [3 5] (out! (+ 1 2) (+ 2 3))))
           (dosync
             (is (= [[3 5]] @-space) "Space contains [3 5]?"))))

(deftest test-eval!
  (testing "(eval! expr)"
           (initialize!)
           (let [f (eval! (+ 1 1))]
             (Thread/sleep 100)
             (dosync
               (is (= [[2]] @-space) "Space contains [2]?"))
             (is (= [2] @f))))
  (testing "(eval! expr1 expr2 :x)"
           (initialize!)
           (let [f (eval! 
               (+ 1 1)
               (+ 1 2)
               :x)]
             (Thread/sleep 100)
             (dosync
               (is (= [[2 3 :x]] @-space) "Space contains [2 3 :x]?"))
             (is (= [2 3 :x] @f))))
  (testing "Evaluating N * (eval! expr1 expr2 :value) places N * [expr1-result expr2-result :value] into space."
           (initialize!)
           @(eval! 
               (+ 1 1)
               (+ 1 2)
               :x)
           @(eval! 
               (+ 1 1)
               (+ 1 2)
               :y)
           @(eval!  
               (+ 1 1)
               (+ 1 2)
               :z)
           (Thread/sleep 100)
           (dosync
             (is (= [[2 3 :x] [2 3 :y] [2 3 :z]] @-space))))
  (testing "(eval! (out! (+ 1 0) (+ 1 1)) :x :y :z)"
           (initialize!)
           (eval! (out! (+ 1 0) (+ 1 1)) :x :y :z)
           (dosync
             (is (= [[1 2] [[1 2] :x :y :z]] @-space)))))

#_(deftest test-matcher
  (testing ""
           ()))

#_(deftest test-rd!
  (testing "(out! :x) then (rd! :x)."
           (initialize!)
           (out! :z)
           (out! :y)
           (out! :x)
           (is (= [:x] (safe-ops (rd! [:x]))) "(rd! :x) evals to [:x]?")
           (is (= [[:z] [:y] [:x]] @-space) "Space still contains [:x] after (rd! :x)?"))
  (testing "Evaluating (eval! (rd! :x) :done) blocks untill (out! :x) is evaluated."
           (initialize!)
           (eval! (rd! [:x] :ok) :done)
           (Thread/sleep 100)
           (dosync
             (is (= [] @-space) "Space is still empty because (rd! :x) blocks?")
             (is (= 1 (count @-waitq)) "Wait queue contains one element?"))
           (out! :z)
           (out! :y)
           (out! :x)
           (Thread/sleep 100)
           (dosync
             (is (= [[:z] [:y] [:x] [[:x] :done]] @-space) "Space contains [:x] (not removed by rd!) and [[:x] :done]?")
             (is (= [] @-waitq) "Wait queue is empty?"))))

#_(deftest test-in! 
  (testing "(out! :x) then (in! :x)."
           (initialize!)
           (out! :x)
           (is (= [:x] (safe-ops (in! [:x]))) "(in! :x) evals to [:x]?")
           (is (= [] @-space) "Space is empty after (in! :x)?"))
    (testing "Evaluating (eval! (in! :x) :done) blocks untill (out! :x) is evaluated."
           (initialize!)
           (eval! (in! [:x]) :done)
           (Thread/sleep 100)
           (dosync
             (is (= [] @-space) "Space doesn't contain [:x :done] yet?")
             (is (= 1 (count @-waitq)) "Wait queue contains one element?"))
           (out! :x)
           (Thread/sleep 100)
           (dosync
             (is (= [[:z] [:y] [[:x] :done]] @-space) "Space contains only [:x :done] since in! has removed [:x]?")
             (is (= [] @-waitq) "Wait queue is empty?"))))

#_(deftest test-tuple-ops
  (testing "(out! & args)"
           (initialize!)
           (out! :x :y :z)
           (is (= [[:x :y :z]] @-space) "Space contains a tuple in the form [:x :y :z]?"))
  (testing "(in! & args)"
           (initialize!)
           (out! :x :y :y)
           (out! :x :y :z)
           (is (= [:x :y :z] (safe-op (in! :x :y :z))) "in! evals to [:x :y :z]?"))
  (testing "(rd! & args)"
           (initialize!)
           (out! :x :y :y)
           (out! :x :y :z)
           (is (= [:x :y :z] (safe-op (rd! :x :y :z))) "in! evals to [:x :y :z]?")))

#_(deftest test-wildcards
  (testing "Pattern with _ wildcards"
           (initialize!)
           (out! [:y 1 true])           
           (out! [:x 1 true])           
           (is (= [:x 1 true] (rd! :x _ _)) "Matched (rd! :x _ _)?")))

