(ns concourse-pcf-foundation-resource.foundation-configuration
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]))

(s/def ::director-config map?)

(s/def ::config (s/or :config (s/keys :req-un [::director-config])
                      :none nil?))

(defn print-diff
  "Print a simplified diff of the configurations to *err*"
  [current-config desired-config]
  (binding [*out* *err*]
    (println "Currently deployed configuration:")
    (pprint current-config)
    (println)
    (println "Desired configuration:")
    (pprint desired-config)
    (println)
    (println "Changes required:")
    (match [current-config desired-config]
      [{:director-config _} {:director-config _}] (if (not (= current-config desired-config)) (println "\tDirector configuration update"))
      [_                    {:director-config _}] (println "\tDeploy director")
      [{:director-config _} _]                    (println "\tDestroy director"))))

(s/fdef print-diff
        :args (s/cat :current-config ::config :desired-config ::config)
        :ret nil?)

(defn hash-of
  [config]
  "fake-hash")

(s/fdef hash-of
        :args (s/cat :config ::config)
        :ret string?)
