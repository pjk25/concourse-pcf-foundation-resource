(ns foundation-lib.util-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [foundation-lib.util :as util]))

(s/def ::foo string?)

(s/def ::baz int?)

(s/def ::bar (s/coll-of (s/keys :opt [::baz]) :distinct true))

(s/def ::fuzz map?)

(s/def ::a-map (s/keys :req [::foo]
                       :opt [::fuzz]
                       :opt-un [::bar]))

(deftest only-specd
  (stest/instrument `util/only-specd)

  (is (= (util/only-specd ::a-map {::foo "foostr"
                                   :bar [{::baz 123 :other 1}]
                                   :extra "extra-val"})
         {::foo "foostr"
          :bar [{::baz 123}]}))

  (is (= (util/only-specd ::a-map {::foo "foostr"
                                   :extra "extra-val"})
         {::foo "foostr"}))

  (is (= (util/only-specd ::fuzz {:a 1})
         {:a 1}))

  (is (= (util/only-specd ::a-map {::foo "foostr"
                                   ::fuzz {:a 1}})
         {::foo "foostr"
          ::fuzz {:a 1}})))

(deftest non-specd
  (stest/instrument `util/non-specd)

  (is (= (util/non-specd ::a-map {::foo 1})
         {}))

  (is (= (util/non-specd ::a-map {::foo 1 :monkey 2})
         {:monkey 2}))

  (is (= (util/non-specd ::a-map {::foo 2
                                  :bar [{::baz 1 :tree 3}]})
         {:bar [{:tree 3}]}))

  (is (= (util/non-specd ::fuzz {:a 1})
         {}))

  (is (= (util/non-specd ::a-map {::foo 2
                                  ::fuzz {:a 1}})
         {}))

  (is (= (util/non-specd ::bar [{::baz 1}])
         [])))

(deftest select
  (stest/instrument `util/select)

  (is (= (util/select {:a {:b "any"}}
                      {:a {:b 1}})
         {:a {:b 1}}))

  (is (= (util/select {:b {:c [{:d "any"}]}}
                      {:a 1 :b {:c [{:d 2} {:d 3}]}})
         {:b {:c [{:d 2}, {:d 3}]}})))

(deftest structural-minus
  (stest/instrument `util/structural-minus)

  (is (= {:a ::util/elided}
         (util/structural-minus {:a 1} {})))

  (is (= {:a ::util/elided}
         (util/structural-minus {:a 1 :b 2} {:b 2})))

  (is (= {:a ::util/elided
          :b {:d ::util/elided}}
         (util/structural-minus {:a 1 :b {:c 2 :d 3}} {:b {:c 2}}))))
