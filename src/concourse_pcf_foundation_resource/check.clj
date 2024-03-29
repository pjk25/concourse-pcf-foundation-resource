(ns concourse-pcf-foundation-resource.check
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.core :as core]
            [foundation-lib.deployed-configuration :as deployed-configuration]))

(s/def ::version (s/or :version ::core/version :nil nil?))

(s/def ::payload (s/keys :opt-un [::version]))

(defn check
  [cli-options om payload]

  (when-not (s/valid? ::payload payload)
    (binding [*out* *err*]
      (println "Invalid request body")
      (s/explain ::payload payload)
      (println))
    (throw (ex-info "Invalid request body" payload)))

  (let [raw-deployed-config (core/deployed-configuration cli-options om)
        deployed-config (s/conform ::deployed-configuration/deployed-config raw-deployed-config)]

    (when (= ::s/invalid deployed-config)
      (binding [*out* *err*]
        (println "Internal inconsistency: The deployed foundation configuration is not valid")
        (s/explain ::deployed-configuration/deployed-config raw-deployed-config)
        (println))
      (throw (ex-info "Internal inconsistency: The deployed foundation configuration is not valid" {})))

    (let [current-version (core/current-version om deployed-config)]
      [current-version])))

(s/fdef check
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :payload ::payload)
        :ret (s/coll-of ::core/version))
