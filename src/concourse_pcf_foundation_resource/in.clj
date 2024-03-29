(ns concourse-pcf-foundation-resource.in
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [concourse-pcf-foundation-resource.core :as core]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.digest :as digest]
            [foundation-lib.deployed-configuration :as deployed-configuration]))

(s/def ::payload (s/keys :req-un [::core/version]))

(defn in
  [cli-options om payload]

  (when-not (s/valid? ::payload payload)
    (binding [*out* *err*]
      (println "Invalid request body")
      (s/explain ::payload payload)
      (println))
    (throw (ex-info "Invalid request body" payload)))

  (let [requested-version (:version payload)
        raw-deployed-config (core/deployed-configuration cli-options om)
        deployed-config (s/conform ::deployed-configuration/deployed-config raw-deployed-config)]

    (when (= ::s/invalid deployed-config)
      (binding [*out* *err*]
        (println "Internal inconsistency: The deployed foundation configuration is not valid")
        (s/explain ::deployed-configuration/deployed-config raw-deployed-config)
        (println))
      (throw (ex-info "Internal inconsistency: The deployed foundation configuration is not valid" {})))

    (let [current-version (core/current-version om deployed-config)]
      (if (= current-version requested-version)
        (do
          (let [config-file (io/file (:destination cli-options) "configuration.yml")]
            (if (:debug cli-options)
              (binding [*out* *err*]
                (println "Writing data to" (.toString config-file))))
            (spit config-file (yaml/generate-string deployed-config)))
          {:version current-version :metadata []})
        (throw (ex-info "The requested version is no longer available." {:requested-version requested-version
                                                                         :current-version current-version}))))))

(s/fdef in
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :payload ::payload)
        :ret (s/keys :req-un [::core/version ::core/metadata]))
