(ns concourse-pcf-foundation-resource.in
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.core :as core]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.digest :as digest]))

(defn in
  [cli-options om payload])

(s/fdef in
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :payload (s/keys :req-un [::core/version]))
        :ret (s/keys :req-un [::core/version ::core/metadata]))
