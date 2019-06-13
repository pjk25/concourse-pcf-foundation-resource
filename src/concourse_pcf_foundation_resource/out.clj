(ns concourse-pcf-foundation-resource.out
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :refer [pprint]]
            [concourse-pcf-foundation-resource.core :as core]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.digest :as digest]
            [concourse-pcf-foundation-resource.plan :as plan]
            [clj-yaml.core :as yaml])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(s/def ::dry_run boolean?)

(s/def ::params (s/keys :req-un [::dry_run]))

(defn out
  [cli-options om payload]
  (let [deployed-config (core/deployed-configuration cli-options om)
        desired-config (yaml/parse-string (slurp (io/file (:source cli-options) "configuration.yml")))]

    (when-not (s/valid? ::foundation/config desired-config)
      (binding [*out* *err*]
        (println "The supplied foundation configuration is not valid")
        (s/explain ::foundation/config desired-config)
        (println))
      (throw (ex-info "The supplied foundation configuration is not valid" {})))

    ; TODO: print the diff when there is extra data
    (if-not (= desired-config (foundation/select-writable-config desired-config))
      (throw (ex-info "The supplied foundation configuration contains extraneous data" {})))

    (foundation/print-diff deployed-config desired-config)

    (if-let [the-plan (plan/plan deployed-config desired-config)]
      (do
        (binding [*out* *err*]
          (println "Computed plan:")
          (println (plan/describe-plan the-plan) "\n"))
        (if (or (empty? the-plan) (get-in payload [:params :dry_run]))
          (let [current-version (core/current-version om deployed-config)]
            {:version current-version :metadata []})
          (do
            (core/apply-plan cli-options om the-plan)
            (let [redeployed-config (core/deployed-configuration cli-options om)
                  current-version (core/current-version om redeployed-config)]
              {:version current-version :metadata []}))))
      (throw (ex-info "Cannot formulate a suitable plan that converges towards the desired foundation state" {})))))

(s/fdef out
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :payload (s/keys :opt-un [::params]))
        :ret (s/keys :req-un [::core/version ::core/metadata]))
