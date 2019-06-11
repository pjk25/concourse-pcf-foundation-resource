(ns concourse-pcf-foundation-resource.core
  (:require [clojure.core.match :refer [match]]
            [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.digest :as digest]
            [concourse-pcf-foundation-resource.plan :as plan]
            [clj-yaml.core :as yaml])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(s/def ::opsman_version string?)

(s/def ::configuration_hash string?)

(s/def ::version (s/keys :req-un [::opsman_version] :opt-un [::configuration_hash]))

(s/def ::name string?)

(s/def ::value string?)

(s/def ::metadata (s/coll-of (s/keys :req-un [::name ::value])))

(defn- changes-being-applied?
  [parsed-installations-response]
  (-> parsed-installations-response
      (first)
      (:status)
      (= "running")))

(defn- last-apply-changes-failed?
  [parsed-installations-response]
  (-> parsed-installations-response
      (first)
      (:status)
      (= "failed")))

(defn changes-pending?
  [parsed-pending-changes-response]
  false)

(s/fdef changes-pending?
        :args (s/cat :parsed-pending-changes-response map?)
        :ret boolean?)

(defn- fresh-opsman?
  [parsed-pending-changes-response]
  (let [{:keys [product_changes]} parsed-pending-changes-response]
    (and (= 1 (count product_changes))
         (= "install" (:action (first product_changes)))
         (= "p-bosh" (:identifier (:staged (first product_changes)))))))

(defn deployed-configuration
  [cli-options om]
  (let [installations (json/read-str (om-cli/curl om "/api/v0/installations") :key-fn keyword)]
    (cond
     (changes-being-applied? installations) (throw (ex-info "Changes are currently being applied" {}))
     (last-apply-changes-failed? installations) (throw (ex-info "The last Apply Changes failed" {}))
     :else (let [pending-changes-result (json/read-str (om-cli/curl om "/api/v0/staged/pending_changes") :key-fn keyword)]
             (cond
              (fresh-opsman? pending-changes-result) nil
              (changes-pending? pending-changes-result) (throw (ex-info "Changes are pending" {}))
              :else (yaml/parse-string (om-cli/staged-director-config om)))))))

(s/fdef deployed-configuration
        :args (s/cat :cli-options map?
                     :om ::om-cli/om)
        :ret ::foundation/config)

(defn current-version
  [om deployed-config]
  (let [info (json/read-str (om-cli/curl om "/api/v0/info") :key-fn keyword)]
    (cond-> {:opsman_version (get-in info [:info :version])}
            deployed-config (assoc :configuration_hash
                              (foundation/hash-of deployed-config)))))

(s/fdef current-version
        :args (s/cat :om ::om-cli/om
                     :deployed-config ::foundation/config)
        :ret ::version)

(defn apply-plan
  [cli-options om plan]
  (doseq [step plan]
    (if (:debug cli-options)
      (binding [*out* *err*]
        (println "Performing" (::plan/action step))))
    (doseq [line ((plan/executor step) cli-options om)]
      (if (:debug cli-options)
        (binding [*out* *err*]
          (println line))))))

(s/fdef apply-plan
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :plan ::plan)
        :ret nil?)
