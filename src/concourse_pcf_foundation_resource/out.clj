(ns concourse-pcf-foundation-resource.out
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.core :as core]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.digest :as digest]))

(s/def ::dry_run boolean?)

(s/def ::params (s/keys :req-un [::dry_run]))

(defn out
  [cli-options om payload]
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
