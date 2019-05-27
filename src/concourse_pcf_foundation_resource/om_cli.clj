(ns concourse-pcf-foundation-resource.om-cli
  (:require [clojure.spec.alpha :as s]
            [clojure.java.shell :as shell]))

(s/def ::url string?)

(s/def ::username string?)

(s/def ::password string?)

(s/def ::skip_ssl_validation boolean?)

(s/def ::opsmgr (s/keys :req-un [::url ::username ::password]
                        :opt-un [::skip_ssl_validation]))

(defprotocol Om
  (staged-director-config [this])
  (curl [this path]))

(s/def ::om #(satisfies? Om %))

(defn- sh-om
  [opsmgr & args]
  (let [{:keys [url username password]} opsmgr
        base-args (cond-> ["om" "--target" url "--username" username "--password" password]
                          (:skip_ssl_validation opsmgr) (conj "--skip-ssl-validation"))]
    (apply shell/sh (concat base-args args))))

(deftype OmCli [opsmgr]
  Om
  (staged-director-config [this]
    (let [{:keys [exit out err]} (sh-om opsmgr "staged-director-config")]
      (if (= 0 exit)
        out
        (throw (ex-info err {})))))
  (curl [this path]
    (let [{:keys [exit out err]} (sh-om opsmgr "curl" "--silent" "--path" path)]
      (if (= 0 exit)
        out
        (throw (ex-info err {:path path}))))))
