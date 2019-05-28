(ns concourse-pcf-foundation-resource.core
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.digest :as digest]
            [clj-yaml.core :as yaml])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(s/def ::opsman_version string?)

(s/def ::configuration_hash string?)

(s/def ::version (s/keys :req-un [::opsman_version ::configuration_hash]))

(s/def ::name string?)

(s/def ::value string?)

(s/def ::metadata (s/coll-of (s/keys :req-un [::name ::value])))

(defn- changes-being-applied?
  [parsed-installations-response]
  (-> parsed-installations-response
      (first)
      (:status)
      (= "running")))

(defn- changes-pending?
  [parsed-pending-changes-response]
  (seq parsed-pending-changes-response)) ; idiomatic form of (not (empty? x))

(defn- fresh-opsman?
  [parsed-pending-changes-response]
  (let [{:keys [product_changes]} parsed-pending-changes-response]
    (and (= 1 (count product_changes))
         (= "install" (:action (first product_changes)))
         (= "p-bosh" (:identifier (:staged (first product_changes)))))))

(defn deployed-configuration
  [cli-options om]
  (let [installations (json/read-str (om-cli/curl om "/api/v0/installations") :key-fn keyword)]
    (if (changes-being-applied? installations)
      (throw (ex-info "Changes are currently being applied" {}))
      (let [pending-changes-result (json/read-str (om-cli/curl om "/api/v0/staged/pending_changes") :key-fn keyword)]
        (cond
          (fresh-opsman? pending-changes-result) nil
          (changes-pending? pending-changes-result) (throw (ex-info "Changes are pending" {}))
          :else (yaml/parse-string (om-cli/staged-director-config om)))))))

(defn current-version!
  [cli-options om destination]
  (let [info (json/read-str (om-cli/curl om "/api/v0/info") :key-fn keyword)
        deployed-configuration (deployed-configuration cli-options om)]
    (if deployed-configuration
      (let [config-file (io/file destination "configuration.yml")]
        (if (:debug cli-options)
          (binding [*out* *err*]
            (println "Writing data to" (.toString config-file))))
        (spit config-file (yaml/generate-string deployed-configuration))))
    (cond-> {:opsman_version (get-in info [:info :version])}
            deployed-configuration (assoc :configuration_hash (foundation/hash-of deployed-configuration)))))

(s/fdef current-version
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :destination string?)
        :ret ::version)
