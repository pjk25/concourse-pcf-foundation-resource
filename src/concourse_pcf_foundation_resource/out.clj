(ns concourse-pcf-foundation-resource.out
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [concourse-pcf-foundation-resource.core :as core]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.digest :as digest]
            [concourse-pcf-foundation-resource.yaml :as yaml])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(s/def ::dry_run boolean?)

(s/def ::params (s/keys :req-un [::dry_run]))

(comment (defn- print-diff
           [deployed-config wanted-config-file]
           (if deployed-config
             (let [temp-dir (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
                   destination (.toString temp-dir)
                   config-file (io/file destination "configuration.yml")]
               (spit config-file deployed-config)
               (let [{:keys [exit out err]} (shell/sh "diff"
                                                      (.toString (io/file destination "configuration.yml"))
                                                      (.toString wanted-config-file))]
                 (if (not (= 0 exit))
                   (throw (ex-info "Failed to invoke diff" {:status exit :stdout out :stderr err})))
                 (binding [*out* *err*]
                   (println out)))))))

(defn out
  [cli-options om payload]
  (let [deployed-configuration (core/deployed-configuration cli-options om)]

    (foundation/print-diff deployed-configuration (yaml/read-str (slurp (io/file (:source cli-options) "configuration.yml"))))

    (throw (ex-info "Don't know how to converge upon the desired state." {})))
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
