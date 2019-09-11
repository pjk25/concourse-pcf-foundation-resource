(ns foundation-lib.foundation-configuration-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [foundation-lib.foundation-configuration :as foundation]))

(deftest requires-changes?
  (stest/instrument `foundation/requires-changes?)

  (testing "when what is desired is more precise than what is deployed"
    (is (foundation/requires-changes? {:a 1} {:a 1 :b 2})))

  (testing "when the desired is less precise than what is deployed"
    (is (not (foundation/requires-changes? {:a 1 :b 2} {:a 1}))))

  (testing "responds reasonably for nil args"
    (is (foundation/requires-changes? nil 1))
    (is (foundation/requires-changes? 1 nil))
    (is (not (foundation/requires-changes? nil nil)))))

(deftest select-writable-config
  (is (= {:director-config
          {:az-configuration
           [{:name "foo-az"}]}}
         (foundation/select-writable-config {:director-config
                                             {:az-configuration
                                              [{:name "foo-az" :iaas_configuration_guid "foo-guid"}]}}))))

(comment
  (deftest hash-of
    (is (= 1 (-> (stest/check `foundation/hash-of {:num-tests 10})
                 (stest/summarize-results)
                 :check-passed)))))
