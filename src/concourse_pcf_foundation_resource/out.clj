(ns concourse-pcf-foundation-resource.out
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [concourse-pcf-foundation-resource.core :as core]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.digest :as digest]
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

    (if-let [plan (core/plan deployed-configuration desired-configuration)]
      (do
        (core/apply-plan cli-options om plan)
        (let [temp-dir (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
              destination (.toString temp-dir)
              current-version (core/current-version! cli-options om destination)]
          {:version current-version :metadata []}))
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
                     :payload (s/keys :req-un [::params]))
        :ret (s/keys :req-un [::core/version ::core/metadata]))
