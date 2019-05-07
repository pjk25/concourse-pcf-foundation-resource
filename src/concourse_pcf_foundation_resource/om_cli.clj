(ns concourse-pcf-foundation-resource.om-cli
  (:require [clojure.spec.alpha :as s]
            [clojure.java.shell :as shell]))

(s/def ::url string?)

(s/def ::username string?)

(s/def ::password string?)

(s/def ::opsmgr (s/keys :req-un [::url ::username ::password]))

(s/def ::command #{"staged-director-config"})

(s/def ::om-args (s/cat :opsmgr ::opsmgr :command ::command :args (s/* string?)))

(defn om
  [opsmgr command & args]
  (let [{:keys [url username password]} opsmgr]
    (shell/sh "om" "--target" url "--username" username "--password" password command)))

(s/fdef om
        :args ::om-args)
