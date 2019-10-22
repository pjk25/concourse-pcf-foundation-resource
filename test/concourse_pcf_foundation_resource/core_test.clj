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

(deftest deployed-configuration
  (stest/instrument `core/deployed-configuration)

  (testing "when the last apply changes failed"
    (let [fake-om (reify om-cli/Om
                    (staged-director-config [this]
                      (slurp "resources/fixtures/staged-director-config.yml"))
                    (curl [this path]
                      (condp = path
                        "/api/v0/installations" (slurp "resources/fixtures/curl/installations/failure.json")
                        "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/director_deployed.json")
                        (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))]
      (is (thrown? clojure.lang.ExceptionInfo (core/deployed-configuration {} fake-om)))))

  (testing "when changes are pending"
    (let [fake-om (reify om-cli/Om
                    (staged-director-config [this]
                      (slurp "resources/fixtures/staged-director-config.yml"))
                    (staged-products [this]
                      "[]")
                    (curl [this path]
                      (condp = path
                        "/api/v0/installations" (slurp "resources/fixtures/curl/installations/success.json")
                        "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/docs.json")
                        (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))]
      (is (thrown? clojure.lang.ExceptionInfo (core/deployed-configuration {}  fake-om)))))

  (testing "a fresh opsman with authentication already set up"
    (let [fake-om (reify om-cli/Om
                    (staged-director-config [this]
                      (slurp "resources/fixtures/staged-director-config.yml"))
                    (curl [this path]
                      (condp = path
                        "/api/v0/info" (slurp "resources/fixtures/curl/info.json")
                        "/api/v0/installations" (slurp "resources/fixtures/curl/installations/success.json")
                        "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/fresh_opsman.json")
                        (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))]
      (is (= (core/deployed-configuration {} fake-om)
             {:opsman-version "2.5.4-build.189"}))))

  (testing "when the configuration can be determined"
    (let [fake-om (reify om-cli/Om
                    (staged-director-config [this]
                      (slurp "resources/fixtures/staged-director-config.yml"))
                    (staged-config [this product-name]
                      (if (= product-name "cf")
                        "product-name: cf\n"
                        (throw (ex-info "product not found" {}))))
                    (deployed-products [this]
                      (slurp "resources/fixtures/deployed-products/director_and_cf.json"))
                    (staged-products [this]
                      (slurp "resources/fixtures/staged-products/director_and_cf.json"))
                    (curl [this path]
                      (condp = path
                        "/api/v0/info" (slurp "resources/fixtures/curl/info.json")
                        "/api/v0/installations" (slurp "resources/fixtures/curl/installations/success.json")
                        "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/director_deployed.json")
                        "/api/v0/stemcell_assignments" (slurp "resources/fixtures/curl/stemcell_assignments/missing_stemcell.json")
                        (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))]
      (is (= (keys (core/deployed-configuration {} fake-om))
             [:director-config :products :opsman-version]))
      (is (= (:products (core/deployed-configuration {} fake-om))
             [{:product-name "cf"
               :version "2.5.4"
               :stemcells [{:version "250.48"
                            :os "ubuntu-xenial"}]}])))))

(deftest current-version
  (stest/instrument `core/current-version)

  (testing "a fresh opsman with authentication already set up"
    (let [fake-om (reify om-cli/Om
                    (staged-director-config [this]
                      (slurp "resources/fixtures/staged-director-config.yml"))
                    (curl [this path]
                      (condp = path
                        "/api/v0/info" (slurp "resources/fixtures/curl/info.json")
                        "/api/v0/installations" (slurp "resources/fixtures/curl/installations/success.json")
                        "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/fresh_opsman.json")
                        (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))]
      (is (= (core/current-version fake-om {:opsman-version "2.5.4-build.189"})
             {:hash (format "%x" (hash {:opsman-version "2.5.4-build.189"}))})))))

