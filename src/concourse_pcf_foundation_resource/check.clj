(ns concourse-pcf-foundation-resource.check
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.java.shell :as shell]))

(s/def ::url string?)

(s/def ::username string?)

(s/def ::password string?)

(s/def ::opsmgr (s/keys :req-un [::url ::username ::password]))

(s/def ::source (s/keys :req-un [::opsmgr]))

(s/def ::version string?)

(s/def ::check-payload (s/keys :req-un [::source ::version]))

(defn check
  [check-payload]
  (let [{:keys [source]} check-payload
        {:keys [opsmgr]} source
        {:keys [url username password]} opsmgr]
    (shell/sh "om" "--target" url "--username" username "--password" password "staged-director-config")))

(s/fdef check
        :args (s/cat :check-payload ::check-payload)
        :ret ::version)

