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
  (staged-config [this product-name])
  (deployed-products [this])
  (staged-products [this])
  (available-products [this])
  (curl [this path])
  (configure-director [this config])
  (upload-product [this config file])
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
      ; need to figure out how to print the result of a failed om command
      (apply om (concat base-args args)))))

(defn- sh-om-side-stream-results
  [cli-options opsmgr & args]
  (let [{:keys [url username password]} opsmgr
        base-args (cond-> ["--target" url "--username" username "--password" password]
                    (:skip_ssl_validation opsmgr) (conj "--skip-ssl-validation"))]
    (if (:debug cli-options)
      (binding [*out* *err*]
        (println "Invoking om with" args)))
    (let [p (apply sh-ll/proc "om" (concat base-args args [:redirect-err true]))]
      (if (:debug cli-options)
        (sh-ll/stream-to p :out (System/err)))
      (let [status (sh-ll/exit-code p)]
        (condp = status
          0 "\nom invocation completed successfully."
          (throw (ex-info "om invocation failed" {:code status :args args})))))))

(deftype OmCli [cli-options opsmgr]
  Om
  (staged-director-config [this]
    (sh-om cli-options opsmgr "staged-director-config" "--no-redact"))

  (staged-config [this product-name]
    (sh-om cli-options opsmgr "staged-config" "--product-name" product-name "--include-credentials"))

  (deployed-products [this]
    (sh-om cli-options opsmgr "deployed-products" "-f" "json"))

  (staged-products [this]
    (sh-om cli-options opsmgr "staged-products" "-f" "json"))

  (available-products [this]
    (sh-om cli-options opsmgr "available-products" "-f" "json"))

  (curl [this path]
    (sh-om cli-options opsmgr "curl" "--silent" "--path" path))

  (configure-director [this config]
    (let [config-file (-> (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
                          (.toString)
                          (io/file "director-config.yml"))]
      (spit config-file (yaml/generate-string config))
      (sh-om-side-stream-results cli-options opsmgr "configure-director" "--config" (.toString config-file))))

  (upload-product [this config file]
    (sh-om-side-stream-results cli-options opsmgr "upload-product" "--product-version" (:version config) "--product" file))

  (stage-product [this config]
    (sh-om cli-options opsmgr "stage-product" "--product-name" (:product-name config) "--product-version" (:version config)))

  (configure-product [this config]
    (let [config-file (-> (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
                          (.toString)
                          (io/file "product-config.yml"))]
      (spit config-file (yaml/generate-string (dissoc config :source :version)))
      (sh-om-side-stream-results cli-options opsmgr "configure-product" "--config" (.toString config-file))))

  (apply-changes [this options]
    (apply sh-om-side-stream-results cli-options opsmgr "apply-changes" options)))

(s/fdef staged-director-config
        :args (s/cat :this ::om)
        :ret string?)

(s/fdef staged-config
  :args (s/cat :this ::om
               :product-name string?)
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

(s/def ::version string?)

(s/fdef upload-product
        :args (s/cat :this ::om
                     :config (s/keys :req-un [::product-name ::version])
                     :file string?)
        :ret string?)

(s/fdef stage-product
        :args (s/cat :this ::om
                     :config (s/keys :req-un [::product-name ::version]))
        :ret string?)

(s/fdef configure-product
        :args (s/cat :this ::om
                     :config map?)
        :ret string?)

(s/fdef apply-changes
        :args (s/cat :this ::om
                     :options (s/* string?))
        :ret string?)
