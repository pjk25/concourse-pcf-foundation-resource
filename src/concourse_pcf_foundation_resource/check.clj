(ns concourse-pcf-foundation-resource.check
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.core :as core]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn check
  [cli-options om payload]
  (let [deployed-config (core/deployed-configuration cli-options om)
        current-version (core/current-version om deployed-config)]
    [current-version]))

(s/fdef check
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :payload (s/keys :opt-un [::core/version]))
        :ret (s/coll-of ::core/version))
