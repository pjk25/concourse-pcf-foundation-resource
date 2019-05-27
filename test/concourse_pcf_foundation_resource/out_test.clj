(ns concourse-pcf-foundation-resource.out-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.data.json :as json]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.out :as out]
            [concourse-pcf-foundation-resource.digest :as digest]))

(deftest out
  (stest/instrument `out/out)

  (testing "a fresh opsman with authentication already set up"
    (let [fake-om (reify om-cli/Om
                    (staged-director-config [this]
                      (slurp "resources/fixtures/staged-director-config.yml"))
                    (curl [this path]
                      (condp = path
                                        ; "/api/v0/info" (slurp "resources/fixtures/curl/info.json")
                                        ; "/api/v0/installations" (slurp "resources/fixtures/curl/installations.json")
                                        ; "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/fresh_opsman.json")
                        (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))]
      (is (= (out/out {:source ""} fake-om {})
             {:version {:opsman_version "2.5.4-build.189"}
              :metadata []})))))
