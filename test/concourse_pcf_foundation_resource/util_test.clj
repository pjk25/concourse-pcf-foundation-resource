(ns concourse-pcf-foundation-resource.util-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [concourse-pcf-foundation-resource.util :as util]))

(s/def ::foo string?)

(s/def ::baz int?)

(s/def ::bar (s/keys :opt [::baz]))

(s/def ::a-map (s/keys :req [::foo] :opt-un [::bar]))

(deftest only-specd
  (stest/instrument `util/only-specd)

  (is (= (util/only-specd ::a-map {::foo "foostr"
                                   :bar {::baz 123}
                                   :extra "extra-val"})
         {::foo "foostr"
          :bar {::baz 123}})))
