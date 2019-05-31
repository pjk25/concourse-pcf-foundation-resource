(ns concourse-pcf-foundation-resource.core-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.digest :as digest]
            [concourse-pcf-foundation-resource.core :as core])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(comment (deftest current-version
           (stest/instrument `core/current-version)

           (testing "a fresh opsman with authentication already set up"
             (let [fake-om (reify om-cli/Om
                             (staged-director-config [this]
                               (slurp "resources/fixtures/staged-director-config.yml"))
                             (curl [this path]
                               (condp = path
                                 "/api/v0/info" (slurp "resources/fixtures/curl/info.json")
                                 "/api/v0/installations" (slurp "resources/fixtures/curl/installations.json")
                                 "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/fresh_opsman.json")
                                 (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))
                   temp-dir (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
                   destination (.toString temp-dir)]
               (is (= (core/current-version! {} fake-om destination)
                      {:opsman_version "2.5.4-build.189"}))))

           (testing "when changes are being applied"
             (let [fake-om (reify om-cli/Om
                             (staged-director-config [this]
                               (slurp "resources/fixtures/staged-director-config.yml"))
                             (curl [this path]
                               (condp = path
                                 "/api/v0/installations" (slurp "resources/fixtures/installations_running.json")
                                 "/api/v0/staged/pending_changes" (slurp "resources/fixtures/pending_changes.json")
                                 (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))]
               (is (thrown? clojure.lang.ExceptionInfo (core/current-version! {} fake-om "")))))

           (testing "when changes are pending"
             (let [fake-om (reify om-cli/Om
                             (staged-director-config [this]
                               (slurp "resources/fixtures/staged-director-config.yml"))
                             (curl [this path]
                               (condp = path
                                 "/api/v0/installations" (slurp "resources/fixtures/curl/installations.json")
                                 "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/docs.json")
                                 (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))]
               (is (thrown? clojure.lang.ExceptionInfo (core/current-version! {} fake-om "")))))

           (testing "when the version can be determined"
             (let [fake-om (reify om-cli/Om
                             (staged-director-config [this]
                               (slurp "resources/fixtures/staged-director-config.yml"))
                             (curl [this path]
                               (condp = path
                                 "/api/v0/info" (slurp "resources/fixtures/curl/info.json")
                                 "/api/v0/installations" (slurp "resources/fixtures/curl/installations.json")
                                 "/api/v0/staged/pending_changes" (slurp "resources/fixtures/pending_changes_none.json")
                                 (throw (ex-info (slurp "resources/fixtures/curl_not_found.html") {:path path})))))
                   temp-dir (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
                   destination (.toString temp-dir)]
               (is (= (core/current-version! {} fake-om destination)
                      [{:opsman_version "2.5.4-build.189"
                        :configuration_hash (digest/sha256 (slurp "resources/fixtures/staged-director-config.yml"))}]))
               (is (.exists (io/file destination "configuration.yml")))))))
