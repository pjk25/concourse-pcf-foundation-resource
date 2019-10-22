(ns concourse-pcf-foundation-resource.plan-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.plan :as plan]
            [clj-yaml.core :as yaml]))

(deftest executor
  (testing "stage-product"
    (let [fake-om (reify om-cli/Om
                    (available-products [this] "[{\"name\": \"p-healthwatch\",\"version\": \"1.6.3-build.3\"}]")
                    (stage-product [this config exact-version]
                      (assert (= "1.6.3-build.3" exact-version))
                      "success"))]
      (is (= ((plan/executor {::plan/action :stage-product
                              ::plan/config {:product-name "p-healthwatch"
                                             :version "1.6.3"
                                             :source {:pivnet-file-glob "*.pivotal"}}}) {} fake-om)
             "success")))))

(deftest plan
  (stest/instrument `plan/plan)

  (testing "deploying the director"
    (let [fake-om (reify om-cli/Om
                    (staged-director-config [this]
                      (slurp "resources/fixtures/staged-director-config.yml"))
                    (available-products [this] "[]")
                    (staged-products [this] "[]")
                    (deployed-products [this] "[]")
                    (curl [this path]
                      (condp = path
                        "/api/v0/info" (slurp "resources/fixtures/curl/info.json")
                        "/api/v0/installations" (slurp "resources/fixtures/curl/installations.json")
                        "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/fresh_opsman.json")
                        "/api/v0/stemcell_assignments" (slurp "resources/fixtures/curl/stemcell_assignments/missing_stemcell.json")
                        (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))
          desired-config (yaml/parse-string (slurp "resources/fixtures/desired-config/configuration.yml") :key-fn keyword)]
      (is (s/valid? ::plan/plan (plan/plan fake-om {:opsman-version "2.5.4"} desired-config)))
      (is (= [:configure-director :upload-stemcell :upload-product :stage-product :configure-product :apply-changes]
             (map ::plan/action (plan/plan fake-om {:opsman-version "2.5.4"} desired-config))))))

  (testing "director already deployed"
    (let [fake-om (reify om-cli/Om
                    (staged-director-config [this]
                      (slurp "resources/fixtures/staged-director-config.yml"))
                    (available-products [this] "[]")
                    (staged-products [this] "[]")
                    (deployed-products [this]
                      (slurp "resources/fixtures/deployed-products/just_director.json"))
                    (curl [this path]
                      (condp = path
                        "/api/v0/info" (slurp "resources/fixtures/curl/info.json")
                        "/api/v0/installations" (slurp "resources/fixtures/curl/installations.json")
                        "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/fresh_opsman.json")
                        "/api/v0/stemcell_assignments" (slurp "resources/fixtures/curl/stemcell_assignments/missing_stemcell.json")
                        (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))
          deployed-config (yaml/parse-string (slurp "resources/fixtures/director-deployed.yml"))
          desired-config (yaml/parse-string (slurp "resources/fixtures/desired-config/configuration.yml") :key-fn keyword)]
      (is (s/valid? ::plan/plan (plan/plan fake-om deployed-config desired-config)))
      (is (= [:upload-stemcell :upload-product :stage-product :configure-product :apply-changes] (map ::plan/action (plan/plan fake-om deployed-config desired-config))))))

  (testing "when there is nothing to do"
    (let [fake-om (reify om-cli/Om
                    (staged-director-config [this]
                      (slurp "resources/fixtures/staged-director-config.yml"))
                    (deployed-products [this]
                      (slurp "resources/fixtures/deployed-products/just_director.json"))
                    (curl [this path]
                      (condp = path
                        "/api/v0/info" (slurp "resources/fixtures/curl/info.json")
                        "/api/v0/installations" (slurp "resources/fixtures/curl/installations.json")
                        "/api/v0/staged/pending_changes" (slurp "resources/fixtures/curl/pending_changes/fresh_opsman.json")
                        "/api/v0/stemcell_assignments" (slurp "resources/fixtures/curl/stemcell_assignments/director_and_foo.json")
                        (throw (ex-info (slurp "resources/fixtures/curl/not_found.html") {:path path})))))
          desired-config (yaml/parse-string (slurp "resources/fixtures/desired-config/configuration.yml")
                                            :key-fn keyword)]
      (is (s/valid? ::plan/plan (plan/plan fake-om desired-config desired-config)))
      (is (= [] (plan/plan fake-om desired-config desired-config))))))
