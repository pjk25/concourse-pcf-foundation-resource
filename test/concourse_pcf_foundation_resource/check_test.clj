(ns concourse-pcf-foundation-resource.check-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.data.json :as json]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.check :as check]))

(def fake-om
  (reify om-cli/Om
    (staged-director-config [this]
      "---\ndirector-config: imagine-yaml")
    (curl [this path]
      (condp = path
        "/api/v0/installations" "\"installations-json\""
        "/api/v0/staged/pending_changes" "\"pending-changes-json\""
        "whatever om curl does on a 404"))))

(deftest check
  (stest/instrument `check/check)
  (is (= (check/check fake-om "fake-version")
         ["a"])))
