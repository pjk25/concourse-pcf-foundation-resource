(ns concourse-pcf-foundation-resource.foundation-configuration-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]))

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
