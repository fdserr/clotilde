(ns clotilde.core-test
  (:use clojure.test
        clotilde.core
        clotilde.innards
        clotilde.tools
        matchure
        #_clj-tuple))

(defmacro safe-ops
  [time-out & exprs]
  `(deref (future ~@exprs) ~time-out nil))

(def test-matcher (fn-match ([[:x ?v]] v) ([_] nil)))
(def test-matcher-nox (fn-match ([[:l ?v]] v) ([_] nil)))
(def test-promise [(promise) test-matcher])
(def test-promise-nox [(promise) test-matcher-nox])
(def test-space [[:z 0] [:y 0] [:x 1] [:x 1]])
(def test-space-nox [[:z 0] [:y 0] [:z 0] [:y 0]])
(def test-space-ref (ref test-space))
(def test-space-ref-nox (ref test-space-nox))

(deftest test-test-tools
  (testing "safe-ops"
           (is (= nil (safe-ops 10 (Thread/sleep 100) nil :ok)))
           (is (= :ok (safe-ops 100 (Thread/sleep 10) nil :ok)))))

;; API tests ==========================

;excess dosync => ensure value of ref?

(deftest test-initialize!
  
  (testing "New blank space."
           (initialize!)
           (dosync
             (is (= [] @-space))
             (is (= [] @-waitq-rds))
             (is (= [] @-waitq-ins))))
  
  (testing "New space with data."
           (initialize! test-space)
           (dosync
             (is (= test-space @-space)))
           (initialize!)
           (dosync
             (is (= [] @-space)))
           (is (thrown? AssertionError 
                        (initialize! :x)))
           (is (thrown? AssertionError 
                        (initialize! [[:x] :x])))
           (is (thrown? AssertionError 
                        (initialize! [[:x] []]))))
  
  (testing "Clear space and queues."
           (initialize! test-space)
           (future (rd! [:l] :l) (in! [:l] :l))           
           (Thread/sleep 20)
           (initialize!)
           (dosync
             (is (= [] @-space))
             (is (= [] @-waitq-rds))
             (is (= [] @-waitq-ins)))))

(deftest test-out!
  
  (testing "Basic out!"
           (initialize!)
           (is (= [:x "xx" 2 2 [:x 1]]
                  (let [x 1] 
                    (out! :x 
                          (str \x \x) 
                          (* 2 x) 
                          (let [a 1 b 1] (+ a b)) 
                          (out! :x 1)))))
           (dosync 
             (is (= [[:x 1] [:x "xx" 2 2 [:x 1]]] @-space))))
  
  (testing "Compare out!s to test-space."
           (initialize!)
           (is (= [:z 0] (out! :z 0)))
           (is (= [:y 0] (out! :y 0)))
           (is (= [:x 1] (out! :x 1)))
           (is (= [:x 1] (out! :x (+ 0 1))))
           (dosync 
             (is (= test-space @-space))))
  
  (testing "out! with waiting in! / rd!"
           (initialize!)
           (let [f (future (in! [:x ?v] v))]
             (out! :y 0)
             (Thread/sleep 20)
             (is (= false (realized? f)))
             (out! :x 1)
             (Thread/sleep 20)
             (is (= 1 @f)))
           (initialize!)
           (let [f (future (rd! [:x ?v] v))]
             (out! :y 0)
             (Thread/sleep 20)
             (is (= false (realized? f)))
             (out! :x 1)
             (Thread/sleep 20)
             (is (= 1 @f)))))

(deftest test-eval!

  (testing "Basic eval!"
           (initialize!)
           (is (= [:x "xx" 2 2 [:x 1]]
                  (let [x 1] 
                    @(eval! :x 
                          (str \x \x) 
                          (* 2 x) 
                          (let [a 1 b 1] (+ a b)) 
                          (out! :x 1)))))
           (dosync 
             (is (= [[:x 1] [:x "xx" 2 2 [:x 1]]] @-space))))
  
  (testing "Compare eval!s to test-space."
           (initialize!)
           (is (= [:z 0] @(eval! :z 0)))
           (is (= [:y 0] @(eval! :y 0)))
           (is (= [:x 1] @(eval! :x 1)))
           (is (= [:x 1] @(eval! :x (+ 0 1))))
           (dosync 
             (is (= test-space @-space))))
  (testing "eval! with waiting in! / rd!"
           (initialize!)
           (let [f (future (in! [:x ?v] v))]
             (eval! :y 0)
             (Thread/sleep 20)
             (is (= false (realized? f)))
             (eval! :x 1)
             (Thread/sleep 20)
             (is (= 1 @f)))
           (initialize!)
           (let [f (future (rd! [:x ?v] v))]
             (eval! :y 0)
             (Thread/sleep 20)
             (is (= false (realized? f)))
             (eval! :x 1)
             (Thread/sleep 20)
             (is (= 1 @f)))))

(deftest test-rd!
 
  (testing "Basic rd!"
           (initialize!)
           (is (= nil (safe-ops 10 (rd! [:x ?v] v))) "No match")
           (initialize!)
           (is (= 1 (safe-ops 10 (out! :x 1) (rd! [:x ?v] v))) "Match found")
           (initialize!)
           (is (= 1 (safe-ops 30
                              (let [r (future (rd! [:x ?v] v))] 
                                (Thread/sleep 20) 
                                (out! :x 1) 
                                @r))) "Wait for match")
           (Thread/sleep 50)
           (dosync
             (is (= [[:x 1]] @-space) "Matching tuple still in space")))
  
  (testing "rd! all of test-space"
           (initialize!)
           (let [f (eval! 
                     [:z (rd! [:z ?v] v)]
                     [:y (rd! [:y ?v] v)]
                     [:x (rd! [:x ?v] v)]
                     [:x (rd! [:x ?v] v)])]
             (out! :x 1) ;;just once covers all rd!
             (out! :y 0)
             (out! :z 0)
             (is (= test-space @f)))
           (Thread/sleep 50)
           (dosync
             (is (= [[:x 1] [:y 0] [:z 0] test-space] @-space)))))

(deftest test-in!
  
  (testing "Basic in!"
           (initialize!)
           (is (= nil (safe-ops 10 (in! [:x ?v] v))) "No match")
           (initialize!)
           (is (= 1 (safe-ops 10 (out! :x 1) (in! [:x ?v] v))) "Match found")
           (initialize!)
           (is (= 1 (safe-ops 30
                              (let [r (future (in! [:x ?v] v))] 
                                (Thread/sleep 20) 
                                (out! :x 1) 
                                @r))) "Wait for match")
           (Thread/sleep 50)
           (dosync
             (is (= [] @-space) "Matching tuple not in space")))
  
  (testing "in! all of test-space"
           (initialize!)
           (let [f (eval! 
                     [:z (in! [:z ?v] v)]
                     [:y (in! [:y ?v] v)]
                     [:x (in! [:x ?v] v)]
                     [:x (in! [:x ?v] v)])]
             (out! :x 1) 
             (out! :x 1) ;;twice to cover all in!
             (out! :y 0)
             (out! :z 0)
             (is (= test-space @f)))
           (Thread/sleep 50)
           (dosync
             (is (= [test-space] @-space)))))



