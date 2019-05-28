(ns concourse-pcf-foundation-resource.check
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.core :as core])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn check
  [cli-options om payload]
  (let [temp-dir (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
        destination (.toString temp-dir)]
    [(core/current-version! cli-options om destination)]))

(s/fdef check
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :payload (s/keys :opt-un [::core/version]))
        :ret (s/coll-of ::core/version))
