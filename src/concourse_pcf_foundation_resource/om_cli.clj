(ns concourse-pcf-foundation-resource.om-cli
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [me.raynes.conch :as sh]
            [me.raynes.conch.low-level :as sh-ll])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(s/def ::url string?)

(s/def ::username string?)

(s/def ::password string?)

(s/def ::skip_ssl_validation boolean?)

(s/def ::opsmgr (s/keys :req-un [::url ::username ::password]
                        :opt-un [::skip_ssl_validation]))

(defprotocol Om
  (staged-director-config [this])
  (curl [this path])
  (configure-director [this config])
  (stage-product [this config])
  (configure-product [this config])
  (apply-changes [this options]))

(s/def ::om #(satisfies? Om %))

(defn- sh-om
  [cli-options opsmgr & args]
  (let [{:keys [url username password]} opsmgr
        base-args (cond-> ["--target" url "--username" username "--password" password]
                    (:skip_ssl_validation opsmgr) (conj "--skip-ssl-validation"))]
    (if (:debug cli-options)
      (binding [*out* *err*]
        (println "Invoking om with" args)))
    (sh/with-programs [om]
      (apply om (concat base-args args)))))

(defn- sh-om-side-stream-results
  [cli-options opsmgr & args]
  (let [{:keys [url username password]} opsmgr
        base-args (cond-> ["--target" url "--username" username "--password" password]
                    (:skip_ssl_validation opsmgr) (conj "--skip-ssl-validation"))]
    (if (:debug cli-options)
      (binding [*out* *err*]
        (println "Invoking om with" args)))
    (let [p (apply sh-ll/proc "om" (concat base-args args))]
      (if (:debug cli-options)
        (binding [*out* *err*]
          (sh-ll/stream-to-out p :out)))
      (let [status (sh-ll/exit-code p)]
        (condp = status
          0 "\nom invocation completed successfully."
          (throw (ex-info "om invocation failed" {:code status :args args})))))))

(deftype OmCli [cli-options opsmgr]
  Om
  (staged-director-config [this]
    (sh-om cli-options opsmgr "staged-director-config" "--no-redact"))

  (curl [this path]
    (sh-om cli-options opsmgr "curl" "--silent" "--path" path))

  (configure-director [this config]
    (let [config-file (-> (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
                          (.toString)
                          (io/file "director-config.yml"))]
      (spit config-file (yaml/generate-string config))
      (sh-om-side-stream-results cli-options opsmgr "configure-director" "--config" (.toString config-file))))

  (stage-product [this config]
    (sh-om cli-options opsmgr "stage-product" "--product-name" (:product-name config) "--product-version" (:product-version config)))

  (configure-product [this config]
    (let [config-file (-> (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
                          (.toString)
                          (io/file "product-config.yml"))]
      (spit config-file (yaml/generate-string config))
      (sh-om-side-stream-results cli-options opsmgr "configure-product" "--config" (.toString config-file))))

  (apply-changes [this options]
    (apply sh-om-side-stream-results cli-options opsmgr "apply-changes" options)))

(s/fdef staged-director-config
        :args (s/cat :this ::om)
        :ret string?)

(s/fdef curl
        :args (s/cat :this ::om
                     :path string?)
        :ret string?)

(s/fdef configure-director
        :args (s/cat :this ::om
                     :config map?)
        :ret string?)

(s/def ::product-name string?)

(s/def ::product-version string?)

(s/fdef stage-product
        :args (s/cat :this ::om
                     :config (s/keys :req-un [::product-name ::product-version]))
        :ret string?)

(s/fdef configure-product
        :args (s/cat :this ::om
                     :config map?)
        :ret string?)

(s/fdef apply-changes
        :args (s/cat :this ::om
                     :options (s/* string?))
        :ret string?)
