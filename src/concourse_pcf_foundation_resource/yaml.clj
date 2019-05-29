(ns concourse-pcf-foundation-resource.yaml
  (:require [clojure.java.io :as io])
  (:import [org.yaml.snakeyaml Yaml]
           [org.yaml.snakeyaml.constructor SafeConstructor]))

(defn read-str
  [str]
  (let [yaml (new Yaml (new SafeConstructor))]
    (with-open [r (io/reader (.getBytes str "UTF-8"))]
      (.load yaml r))))

(defn write-file
  [file contents]
  (let [yaml (new Yaml)]
    (with-open [w (io/writer file)]
      (.dump yaml contents w))))
