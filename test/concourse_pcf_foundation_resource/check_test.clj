(ns concourse-pcf-foundation-resource.check-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.data.json :as json]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.check :as check]
            [concourse-pcf-foundation-resource.digest :as digest]))

(deftest check
  (stest/instrument `check/check)
  (testing "a fresh opsman with authentication already set up"
    (let [fake-om (reify om-cli/Om
                    (staged-director-config [this]
                      (slurp "resources/fixtures/staged-director-config.yml"))
                    (curl [this path]
                      (condp = path
                        "/api/v0/info" (slurp "resources/fixtures/curl/info.json")
                        "/api/v0/installations" (slurp "resources/fixtures/curl/installations.json")
                        "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/fresh_opsman.json")
                        (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))]
      (is (= (check/check {} fake-om {})
             [{:opsman_version "2.5.4-build.189"}]))))
  (comment (testing "when changes are being applied"
             (let [fake-om (reify om-cli/Om
                             (staged-director-config [this]
                               (slurp "resources/fixtures/staged-director-config.yml"))
                             (curl [this path]
                               (condp = path
                                 "/api/v0/installations" (slurp "resources/fixtures/installations_running.json")
                                 "/api/v0/staged/pending_changes" (slurp "resources/fixtures/pending_changes.json")
                                 (throw (ex-info (slurp "resources/fixtures/curl_not_found.html") {:path path})))))]
               (is (thrown? clojure.lang.ExceptionInfo (check/check {} fake-om {}))))))

  (comment (testing "when changes are pending"
             (let [fake-om (reify om-cli/Om
                             (staged-director-config [this]
                               (slurp "resources/fixtures/staged-director-config.yml"))
                             (curl [this path]
                               (condp = path
                                 "/api/v0/installations" (slurp "resources/fixtures/installations.json")
                                 "/api/v0/staged/pending_changes" (slurp "resources/fixtures/pending_changes.json")
                                 (throw (ex-info (slurp "resources/fixtures/curl_not_found.html") {:path path})))))]
               (is (thrown? clojure.lang.ExceptionInfo (check/check {} fake-om {}))))))

  (comment (testing "when the version can be determined"
             (let [fake-om (reify om-cli/Om
                             (staged-director-config [this]
                               (slurp "resources/fixtures/staged-director-config.yml"))
                             (curl [this path]
                               (condp = path
                                 "/api/v0/installations" (slurp "resources/fixtures/installations.json")
                                 "/api/v0/staged/pending_changes" (slurp "resources/fixtures/pending_changes_none.json")
                                 (throw (ex-info (slurp "resources/fixtures/curl_not_found.html") {:path path})))))]
               (is (= (check/check {} fake-om {})
                      [{:staged-director-config (digest/sha256 (slurp "resources/fixtures/staged-director-config.yml"))}]))))))
