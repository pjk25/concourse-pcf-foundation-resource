(ns concourse-pcf-foundation-resource.check-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.data.json :as json]
            [concourse-pcf-foundation-resource.check :as check]))

(defn fake-om
  [opsmgr command & args]
  (if (= opsmgr {:url "opsman.company.com"
                 :username "opsman-user"
                 :password "a-password"})
      "imagine-some-yaml"))

(deftest check
  (stest/instrument `check/check)
  (is (= (check/check fake-om {:source {:opsmgr {:url "opsman.company.com"
                                                 :username "opsman-user"
                                                 :password "a-password"}}
                               :version "fake-version"})
         ["a"])))
