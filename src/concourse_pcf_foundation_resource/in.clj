(ns concourse-pcf-foundation-resource.in
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [concourse-pcf-foundation-resource.core :as core]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.digest :as digest]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]))

(defn in
  [cli-options om payload]
  (let [requested-version (:version payload)
        deployed-config (core/deployed-configuration cli-options om)
        info (json/read-str (om-cli/curl om "/api/v0/info") :key-fn keyword)
        current-version (cond-> {:opsman_version (get-in info [:info :version])}
                          deployed-config (assoc :configuration_hash (foundation/hash-of deployed-config)))]
    (if (= current-version requested-version)
      (do
        (let [config-file (io/file (:destination cli-options) "configuration.yml")]
          (if (:debug cli-options)
            (binding [*out* *err*]
              (println "Writing data to" (.toString config-file))))
          (spit config-file (yaml/generate-string deployed-config)))
        {:version current-version :metadata []})
      (throw (ex-info "The requested version is no longer available." {:version requested-version})))))

(s/fdef in
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :payload (s/keys :req-un [::core/version]))
        :ret (s/keys :req-un [::core/version ::core/metadata]))
