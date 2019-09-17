(ns concourse-pcf-foundation-resource.digest
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s])
  (:import [java.security MessageDigest]))

(defn- hex [s]
  (string/join (map #(format "%02x" %) (.getBytes s "UTF-8"))))

(defn sha256
  [s]
  (let [md (MessageDigest/getInstance "SHA-256")
        digest (.digest md (.getBytes s "UTF-8"))]
    (hex digest)))

(s/fdef sha256
        :args (s/cat :string string?)
        :ret string?)
