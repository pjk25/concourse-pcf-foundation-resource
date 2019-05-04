(ns concourse-pcf-foundation-resource.core
  (:require [clojure.data.json :as json]
            [concourse-pcf-foundation-resource.check :as check]))

(defn check [options]
  (json/write [(check/check (json/read *in*))] *out*))

(defn in [options]
  (println "in"))

(defn out [options]
  (println "out"))
