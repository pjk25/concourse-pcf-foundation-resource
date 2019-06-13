(ns concourse-pcf-foundation-resource.in-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.in :as in]
            [concourse-pcf-foundation-resource.digest :as digest])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def fake-om
  (reify om-cli/Om
    (staged-director-config [this]
      (slurp "resources/fixtures/staged-director-config.yml"))
    (curl [this path]
      (condp = path
        "/api/v0/info" (slurp "resources/fixtures/curl/info.json")
        "/api/v0/installations" (slurp "resources/fixtures/curl/installations.json")
        "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/fresh_opsman.json")
        (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path}))))))

(deftest in
  (stest/instrument `in/in)

  (testing "with a fresh opsman with authentication already set up"
    (let [temp-dir (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
          destination (.toString temp-dir)]
      (is (= (in/in {:destination destination} fake-om {:version {:opsman_version "2.5.4-build.189"
                                                                  :configuration_hash "ff19274a"}})
             {:version {:opsman_version "2.5.4-build.189"
                        :configuration_hash "ff19274a"}
              :metadata []}))))

  (testing "when the version does not exist"
    (let [temp-dir (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
          destination (.toString temp-dir)]
      (is (thrown? clojure.lang.ExceptionInfo
                   (in/in {:destination destination}
                          fake-om
                          {:version {:opsman_version "2.5.4-build.189"
                                     :configuration_hash "some-fake-hash"}}))))))
