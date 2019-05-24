(ns concourse-pcf-foundation-resource.check
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.digest :as digest]))

(s/def ::staged-director-config string?)

(s/def ::version (s/keys :req-un [::staged-director-config]))

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

(defn check
  [cli-options om payload]
  (let [installations (json/read-str (om-cli/curl om "/api/v0/installations") :key-fn keyword)]
    (if (changes-being-applied? installations)
      (throw (ex-info "Changes are currently being applied" {}))
      (let [pending-changes-result (json/read-str (om-cli/curl om "/api/v0/staged/pending_changes") :key-fn keyword)]
        (cond
          (fresh-opsman? pending-changes-result) (let [info (json/read-str (om-cli/curl om "/api/v0/info") :key-fn keyword)]
                                                   [{:opsman_version (get-in info [:info :version])}])
          (changes-pending? pending-changes-result) (throw (ex-info "Changes are pending" {}))
          :else (let [config-result (om-cli/staged-director-config om)]
                  (digest/sha256 config-result)))))))

(s/fdef check
        :args (s/cat :cli-options map?
                     :om ::om-cli/om
                     :payload (s/keys :opt-un [::version]))
        :ret (s/coll-of ::version))

