(ns concourse-pcf-foundation-resource.om-cli
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [me.raynes.conch :as sh])
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

(deftype OmCli [cli-options opsmgr]
  Om
  (staged-director-config [this]
    (sh-om cli-options opsmgr "staged-director-config"))

  (curl [this path]
    (sh-om cli-options opsmgr "curl" "--silent" "--path" path))

  (configure-director [this config]
    (let [config-file (-> (Files/createTempDirectory "concourse-pcf-foundation-resource-" (into-array FileAttribute []))
                          (.toString)
                          (io/file "director-config.yml"))]
      (spit config-file (yaml/generate-string config))
      (sh-om cli-options opsmgr "configure-director" "--config" (.toString config-file) {:seq true})))

  (apply-changes [this options]
    (apply sh-om cli-options opsmgr "apply-changes" (concat options [{:seq true}]))))

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
        :ret (s/* string?))

(s/fdef apply-changes
        :args (s/cat :this ::om
                     :options (s/* string?))
        :ret (s/* string?))
