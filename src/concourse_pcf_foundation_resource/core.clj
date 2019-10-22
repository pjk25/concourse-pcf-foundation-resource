(ns concourse-pcf-foundation-resource.core
  (:require [clojure.core.match :refer [match]]
            [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [foundation-lib.query :as foundation-query]
            [foundation-lib.deployed-configuration :as foundation-deployed-configuration]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.digest :as digest]
            [concourse-pcf-foundation-resource.plan :as plan]
            [concourse-pcf-foundation-resource.query.pending-changes :as pending-changes]
            [clj-yaml.core :as yaml])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(s/def ::hash string?)

(s/def ::version (s/keys :req-un [::hash]))

(s/def ::name string?)

(s/def ::value string?)

(s/def ::metadata (s/coll-of (s/keys :req-un [::name ::value])))

(defn- changes-being-applied?
  [parsed-installations-response]
  (-> parsed-installations-response
      (:installations)
      (first)
      (:status)
      (= "running")))

(defn- last-apply-changes-failed?
  [parsed-installations-response]
  (-> parsed-installations-response
      (:installations)
      (first)
      (:status)
      (= "failed")))

(defn- fixup-staged-director-config
  "Map the shape of the response from 'om staged-director-config' to that of 'om configure-director'"
  [config]
  (-> config
      (assoc-in [:properties-configuration :iaas_configuration]
                (first (:iaas-configurations config)))
      (dissoc :iaas-configurations)))

(defn- product-config
  [cli-options om stemcell-assignments deployed-product]
  (let [{:keys [name version]} deployed-product
        product-config (yaml/parse-string (om-cli/staged-config om name))
        stemcell-assignment (->> stemcell-assignments
                                 (:products)
                                 (filter #(= name (:identifier %)))
                                 (first))]
    (-> product-config
        (assoc :version version)
        (assoc :stemcells [{:version (:deployed_stemcell_version stemcell-assignment)
                            :os (:required_stemcell_os stemcell-assignment)}]))))

(defn- deployed-config
  [cli-options om]
  (let [stemcell-assignments (json/read-str (om-cli/curl om "/api/v0/stemcell_assignments") :key-fn keyword)
        director-config (fixup-staged-director-config (yaml/parse-string (om-cli/staged-director-config om)))
        deployed-products (remove #(= "p-bosh" (:name %))
                                  (json/read-str (om-cli/deployed-products om) :key-fn keyword))
        product-configs (map #(product-config cli-options om stemcell-assignments %) deployed-products)
        full-config (cond-> {:director-config director-config}
                      (seq product-configs) (assoc :products product-configs))]
    (foundation-query/select-writable-config full-config)))

(defn deployed-configuration
  [cli-options om]
  (let [installations (json/read-str (om-cli/curl om "/api/v0/installations") :key-fn keyword)
        info (json/read-str (om-cli/curl om "/api/v0/info") :key-fn keyword)
        opsman-version (get-in info [:info :version])]
    (cond
      (changes-being-applied? installations) (throw (ex-info "Changes are currently being applied" {}))
      (last-apply-changes-failed? installations) (throw (ex-info "The last Apply Changes failed" {}))
      :else (let [pending-changes-result (json/read-str (om-cli/curl om "/api/v0/staged/pending_changes") :key-fn keyword)]
              (match [(pending-changes/interpret pending-changes-result)]
                [:fresh-opsman] {:opsman-version opsman-version}
                [:yes] (throw (ex-info "Changes are pending" {}))
                [:no] (assoc (deployed-config cli-options om)
                             :opsman-version opsman-version))))))

(s/fdef deployed-configuration
        :args (s/cat :cli-options map?
                     :om ::om-cli/om)
        :ret ::foundation-deployed-configuration/deployed-config)

(defn current-version
  [om deployed-config]
  (let [info (json/read-str (om-cli/curl om "/api/v0/info") :key-fn keyword)]
    {:hash (format "%x" (hash deployed-config))}))

(s/fdef current-version
        :args (s/cat :om ::om-cli/om
                     :deployed-config ::foundation-deployed-configuration/deployed-config)
        :ret ::version)

(defn apply-plan
  [cli-options om plan]
  (doseq [step plan]
    (if (:debug cli-options)
      (binding [*out* *err*]
        (println "Performing" (::plan/action step))))
    (let [result ((plan/executor step) cli-options om)]
      (if (:debug cli-options)
        (binding [*out* *err*]
          (println result))))))

(s/fdef apply-plan
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :plan ::plan)
        :ret nil?)
