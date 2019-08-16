(ns concourse-pcf-foundation-resource.plan-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.plan :as plan]
            [clj-yaml.core :as yaml]))

(deftest plan
  (stest/instrument `plan/plan)

  (testing "deploying the director"
    (let [desired-config (yaml/parse-string (slurp "resources/fixtures/desired-config/configuration.yml") :key-fn keyword)]
      (is (s/valid? ::plan/plan (plan/plan {} desired-config)))
      (is (= [:configure-director :apply-changes] (map ::plan/action (plan/plan {} desired-config))))))

  (testing "adding a tile"
    (let [deployed-config (yaml/parse-string (slurp "resources/fixtures/director-deployed.yml"))
          desired-config (yaml/parse-string (slurp "resources/fixtures/desired-config/configuration.yml") :key-fn keyword)]
      (is (s/valid? ::plan/plan (plan/plan deployed-config desired-config)))
      (is (= [:stage-product :configure-product :apply-changes] (map ::plan/action (plan/plan deployed-config desired-config))))))

  (testing "when there is nothing to do"
    (let [desired-config (yaml/parse-string (slurp "resources/fixtures/desired-config/configuration.yml") :key-fn keyword)]
      (is (s/valid? ::plan/plan (plan/plan desired-config desired-config)))
      (is (= [] (plan/plan desired-config desired-config))))))
