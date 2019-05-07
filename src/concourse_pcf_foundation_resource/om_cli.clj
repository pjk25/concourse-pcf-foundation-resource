(ns concourse-pcf-foundation-resource.om-cli
  (:require [clojure.spec.alpha :as s]
            [clojure.java.shell :as shell]))

(s/def ::url string?)

(s/def ::username string?)

(s/def ::password string?)

(s/def ::opsmgr (s/keys :req-un [::url ::username ::password]))

(defprotocol Om
  (staged-director-config [this])
  (curl [this path]))

(s/def ::om #(satisfies? Om %))

(deftype OmCli [opsmgr]
  Om
  (staged-director-config [this]
    (let [{:keys [url username password]} opsmgr
          {:keys [exit out err]} (shell/sh "om"
                                           "--target" url
                                           "--username" username
                                           "--password" password
                                           "staged-director-config")]
      (if (= 0 exit)
        out
        (throw (Exception. err)))))
  (curl [this path]
    (let [{:keys [url username password]} opsmgr
          {:keys [exit out err]} (shell/sh "om"
                                           "--target" url
                                           "--username" username
                                           "--password" password
                                           "curl"
                                           "--path" path)]
      (if (= 0 exit)
        out
        (throw (Exception. err))))))
