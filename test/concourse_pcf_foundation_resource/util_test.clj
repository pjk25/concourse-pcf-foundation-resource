(ns concourse-pcf-foundation-resource.util-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [concourse-pcf-foundation-resource.util :as util]))

(s/def ::foo string?)

(s/def ::baz int?)

(s/def ::bar (s/coll-of (s/keys :opt [::baz]) :distinct true))

(s/def ::a-map (s/keys :req [::foo] :opt-un [::bar]))

(deftest only-specd
  (stest/instrument `util/only-specd)

  (is (= (util/only-specd ::a-map {::foo "foostr"
                                   :bar [{::baz 123 :other 1}]
                                   :extra "extra-val"})
         {::foo "foostr"
          :bar [{::baz 123}]}))

  (is (= (util/only-specd ::a-map {::foo "foostr"
                                   :extra "extra-val"})
         {::foo "foostr"})))

(deftest structural-minus
  (stest/instrument `util/structural-minus)

  (is (= {:a ::util/eluded}
         (util/structural-minus {:a 1} {})))

  (is (= {:a ::util/eluded}
         (util/structural-minus {:a 1 :b 2} {:b 2})))

  (is (= {:a ::util/eluded
          :b {:d ::util/eluded}}
         (util/structural-minus {:a 1 :b {:c 2 :d 3}} {:b {:c 2}}))))
