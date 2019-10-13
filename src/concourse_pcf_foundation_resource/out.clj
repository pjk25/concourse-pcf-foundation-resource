(ns concourse-pcf-foundation-resource.out
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :refer [pprint]]
            [concourse-pcf-foundation-resource.core :as core]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [foundation-lib.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.digest :as digest]
            [concourse-pcf-foundation-resource.plan :as plan]
            [foundation-lib.util :as util]
            [clj-yaml.core :as yaml])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(s/def ::file string?)

(s/def ::dry_run boolean?)

(s/def ::params (s/keys :req-un [::file]
                        :opt-un [::dry_run]))

(s/def ::payload (s/keys :req-un [::params]))

(defn out
  [cli-options om payload]

  (when-not (s/valid? ::payload payload)
    (binding [*out* *err*]
      (println "Invalid request body")
      (s/explain ::payload payload)
      (println))
    (throw (ex-info "Invalid request body" payload)))

  (let [raw-deployed-config (core/deployed-configuration cli-options om)
        raw-desired-config (yaml/parse-string (slurp (io/file (:source cli-options) (:file (:params payload)))))
        deployed-config (s/conform ::foundation/deployed-config raw-deployed-config)
        desired-config (s/conform ::foundation/desired-config raw-desired-config)]

    (let [config-dir (-> (Files/createTempDirectory "concourse-pcf-foundation-resource-"
                                                    (into-array FileAttribute []))
                         (.toFile))]
      (if (:debug cli-options)
        (binding [*out* *err*]
          (println "Writing configuration files to " (.toString config-dir))))
      (with-open [w (io/writer (io/file config-dir "deployed.edn"))]
        (binding [*out* w]
          (pr deployed-config)))
      (with-open [w (io/writer (io/file config-dir "desired.edn"))]
        (binding [*out* w]
          (pr desired-config))))

    (when (= ::s/invalid deployed-config)
      (binding [*out* *err*]
        (println "Internal inconsistency: The deployed foundation configuration is not valid")
        (s/explain ::foundation/deployed-config raw-deployed-config)
        (println))
      (throw (ex-info "Internal inconsistency: The deployed foundation configuration is not valid" {})))

    (when (= ::s/invalid desired-config)
      (binding [*out* *err*]
        (println "The supplied foundation configuration is not valid")
        (s/explain ::foundation/desired-config raw-desired-config)
        (println))
      (throw (ex-info "The supplied foundation configuration is not valid" {})))

    ; TODO: reject configs where the opsman-version is given and wrong
    (let [extra-config (util/structural-minus desired-config (foundation/select-writable-config desired-config))]
      (when-not (empty? extra-config)
        (binding [*out* *err*]
          (println "The supplied foundation configuration contains extraneous data:")
          (pprint extra-config))
        (throw (ex-info "The supplied foundation configuration contains extraneous data" {}))))

    (if-let [{:keys [path]} (foundation/first-difference (util/select desired-config deployed-config)
                                                         desired-config)]
      (binding [*out* *err*]
        (println (format "Found configuration difference at %s" path))))

    (if-let [the-plan (plan/plan om deployed-config desired-config)]
      (if (or (empty? the-plan) (get-in payload [:params :dry_run]))
        (do
          (binding [*out* *err*]
            (println "No changes required."))
          {:version (core/current-version om deployed-config)
           :metadata []})
        (do
          (binding [*out* *err*]
            (println "Computed plan:")
            (println (plan/describe-plan the-plan) "\n"))
          (core/apply-plan cli-options om the-plan)
          (binding [*out* *err*]
            (println "Recomputing deployed version..."))
          (let [redeployed-config (core/deployed-configuration cli-options om)
                new-version (core/current-version om redeployed-config)]
            {:version new-version :metadata []})))
      (throw (ex-info "Cannot formulate a suitable plan that converges towards the desired foundation state" {})))))

(s/fdef out
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :payload ::payload)
        :ret (s/keys :req-un [::core/version ::core/metadata]))
