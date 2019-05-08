(ns concourse-pcf-foundation-resource.check-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.data.json :as json]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.check :as check]))

(def fake-om
  (reify om-cli/Om
    (staged-director-config [this]
      (slurp "resources/fixtures/staged-director-config.yml"))
    (curl [this path]
      (condp = path
        "/api/v0/installations" (slurp "resources/fixtures/installations.json")
        "/api/v0/staged/pending_changes" (slurp "resources/fixtures/pending_changes.json")
        (throw (Exception. (slurp "resources/fixtures/curl_not_found.html")))))))

(deftest check
  (stest/instrument `check/check)
  (is (= (check/check {} fake-om {:version "fake-version"})
         ["a"])))
