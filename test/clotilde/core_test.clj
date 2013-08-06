(ns clotilde.core-test
  (:use clojure.test
        clotilde.core
        clotilde.innards))

(deftest test-initialize!
  (testing "New empty space."
           (initialize!)
           (is (= [] @-space) "Empty space?")
           (is (= [] @-waitq) "Empty queue?"))
  (testing "Clearing space and queue."
           (out! :x)
           (eval! (rd! :y))
           (Thread/sleep 100)
           (initialize!)
           (dosync 
             (is (= [] @-space) "Emptied space?")
             (is (= [] @-waitq) "Emptied queue?"))))

(deftest test-out!
  (testing "Empty space contains :x after evaluating (out! :x)."
           (initialize!)
           (out! :x)
           (dosync
             (is (= [:x] @-space) "Space contains just :x?")))
  (testing "Space can contain several instances of the same value."
           (out! :x)
           (dosync
             (is (= [:x :x] @-space) "Space contains :x twice?"))))

(deftest test-eval!
  (testing "Evaluating (eval! (out! :x)) places :x, and :x again into space."
           (initialize!)
           (eval! (out! :x))
           (Thread/sleep 100)
           (dosync
             (is (= [:x :x] @-space) "Space contains :x twice?")))
  (testing "eval! several out! expressions, place the last expression evaln into space."
           (initialize!)
           (eval! 
               (out! :x)
               (out! :y)
               (out! :z)
               :done)
           (Thread/sleep 100)
           (dosync
             (is (= [:x :y :z :done] @-space) "Space contains :x, :y, :z and :done?"))))

(deftest test-rd!
  (testing "(out! :x) then (rd! :x)."
           (initialize!)
           (out! :x)
           (is (= :x (rd! :x)) "(rd! :x) evals to :x?")
           (is (= [:x] @-space) "Space still contains :x after (rd! :x)?"))
  (testing "Evaluating (eval! (rd! :x) :done) blocks untill (out! :x) is evaluated."
           (initialize!)
           (eval! (rd! :x) :done)
           (Thread/sleep 100)
           (dosync
             (is (= [] @-space) "Space doesn't contain :done or :x yet?")
             (is (= 1 (count @-waitq)) "Wait queue contains one element?"))
           (out! :x)
           (Thread/sleep 100)
           (dosync
             (is (= [:x :done] @-space) "Space contains :x and :done?")
             (is (= [] @-waitq) "Wait queue is empty?"))))

(deftest test-in! 
  (testing "(out! :x) then (in! :x)."
           (initialize!)
           (out! :x)
           (is (= :x (in! :x)) "(in! :x) evals to :x?")
           (is (= [] @-space) "Space is empty after (in! :x)?"))
    (testing "Evaluating (eval! (in! :x) :done) blocks untill (out! :x) is evaluated."
           (initialize!)
           (eval! (in! :x) :done)
           (Thread/sleep 100)
           (dosync
             (is (= [] @-space) "Space doesn't contain :done or :x yet?")
             (is (= 1 (count @-waitq)) "Wait queue contains one element?"))
           (out! :x)
           (Thread/sleep 100)
           (dosync
             (is (= [:done] @-space) "Space contains only :done?")
             (is (= [] @-waitq) "Wait queue is empty?"))))

(deftest test-match-fn
  (testing "Quoted pattern with wildcards '[:x 1 _]"
           (initialize!)
           (out! [:x 1 true])           
           (eval! 
             (let [[_ _ p] (in! '[:x 1 _])]
               [:done p]))
           (Thread/sleep 100)
           (is (= [:done true] @-space) "Space contains [:done true]?")))

