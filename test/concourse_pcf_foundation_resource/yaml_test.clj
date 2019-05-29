(ns concourse-pcf-foundation-resource.yaml-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.core.match :refer [match]]
            [concourse-pcf-foundation-resource.yaml :as yaml]))

(deftest read-str
  (stest/instrument `yaml/read-str)

  (testing "Reading a sample string"
    (is (= (keys (yaml/read-str (slurp "resources/fixtures/staged-director-config.yml")))
           '(:az-configuration
             :network-assignment
             :networks-configuration
             :properties-configuration
             :resource-configuration
             :vmextensions-configuration))))

  (testing "Using core.match on the results"
    (is (= (match [(yaml/read-str (slurp "resources/fixtures/staged-director-config.yml"))]
             [{:az-configuration az-config}] (count az-config))
           3))))
