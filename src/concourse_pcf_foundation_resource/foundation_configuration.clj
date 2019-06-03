(ns concourse-pcf-foundation-resource.foundation-configuration
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]))

(s/def ::director-config map?)

(s/def ::config (s/or :config (s/keys :opt-un [::director-config])
                      :none nil?))

(defn print-diff
  "Print a simplified diff of the configurations to *err*"
  [deployed-config desired-config]
  (binding [*out* *err*]
    (println "Currently deployed configuration:")
    (pprint deployed-config)
    (println)
    (println "Desired configuration:")
    (pprint desired-config)
    (println)))

(s/fdef print-diff
        :args (s/cat :deployed-config ::config
                     :desired-config ::config)
        :ret nil?)

; the yaml
; ---
; director-config: {}
; products:
; - id: cf
;   version: (read-only)
;   tile-path: (write-only)
;   tile-sha: (read-only? (if we can give it))
;   config: {}

;; should be able to write the "plan" definition out to disk-
;; Maybe this should actually be 2 resources, then the put that
;; applys the plan?

(defn hash-of
  [config]
  "fake-hash")

(s/fdef hash-of
        :args (s/cat :config ::config)
        :ret string?)
