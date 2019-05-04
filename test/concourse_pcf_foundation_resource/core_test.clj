(ns concourse-pcf-foundation-resource.core-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.data.json :as json]
            [concourse-pcf-foundation-resource.core :as core]))

(deftest example
  (is (= (with-out-str
           (with-in-str
             (json/write-str {:source {:opsmgr {:url "opsman.company.com"
                                                :username "opsman-user"
                                                :password "a-password"}}
                              :version "fake-version"})
             (core/check {})))
         "a")))
