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
  (let [deployed-configuration (core/deployed-configuration cli-options om)
        desired-configuration (yaml/parse-string (slurp (io/file (:source cli-options) "configuration.yml")))]

    (foundation/print-diff deployed-configuration desired-configuration)

    (if-let [the-plan (plan/plan deployed-configuration desired-configuration)]
      (do
        (binding [*out* *err*]
          (println "Computed plan:")
          (pprint the-plan))
        (if (or (empty? the-plan) (get-in payload [:params :dry_run]))
          (let [current-version (core/current-version om deployed-configuration)]
            {:version current-version :metadata []})
          (do
            (core/apply-plan cli-options om the-plan)
            (let [deployed-configuration (core/deployed-configuration cli-options om)
                  current-version (core/current-version om deployed-configuration)]
              {:version current-version :metadata []}))))
      (throw (ex-info "Cannot formulate a suitable plan that converges towards the desired foundation state" {}))))

  ; retrieve the current version/config
  ; diff with requested config (foundation.yml)
  ; print the diff
  ; print plan
  ; stop if dry-run param is set
  ; execute the changes in the plan
  ; apply changes
)

(s/fdef out
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :payload (s/keys :opt-un [::params]))
        :ret (s/keys :req-un [::core/version ::core/metadata]))
