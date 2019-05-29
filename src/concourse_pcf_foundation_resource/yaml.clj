(ns concourse-pcf-foundation-resource.yaml
  (:require [clojure.java.io :as io]
            [clojure.core.match :refer [match]])
  (:import [org.yaml.snakeyaml Yaml]
           [org.yaml.snakeyaml.constructor SafeConstructor]))

(defn- clojurize
  "Transform the Java data structures from SnakeYaml into Clojure data structures"
  [java-ish]
  (let [LinkedHashMap java.util.LinkedHashMap
        ArrayList java.util.ArrayList]
    (match [(class java-ish)]
      [LinkedHashMap] (into {}
                            (for [e (.entrySet java-ish)]
                              [(keyword (.getKey e)) (clojurize (.getValue e))]))
      [ArrayList] (into []
                        (for [e java-ish] (clojurize e)))
      :else java-ish)))

(defn read-str
  [str]
  (clojurize (let [yaml (new Yaml (new SafeConstructor))]
               (with-open [r (io/reader (.getBytes str "UTF-8"))]
                 (.load yaml r)))))

(defn- javaize
  "Transfrom the Clojure data structures into those that serialize plainly with SnakeYaml"
  [clojure-ish]
  clojure-ish)

(defn write-file
  [file contents]
  (let [yaml (new Yaml)]
    (with-open [w (io/writer file)]
      (.dump yaml (javaize contents) w))))
