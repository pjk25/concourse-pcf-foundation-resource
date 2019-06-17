(ns concourse-pcf-foundation-resource.foundation-configuration
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [concourse-pcf-foundation-resource.util :as util]))

(s/def ::name string?)

(s/def ::az-configuration (s/coll-of (s/keys :req-un [::name]) :distinct true :into #{}))

(s/def ::network-assignment map?)

(s/def ::networks-configuration map?)

(s/def ::director_configuration map?)

(s/def ::security_configuration map?)

(s/def ::syslog_configuration map?)

(s/def ::iaas_configuration map?)

(s/def ::properties-configuration (s/keys :opt-un [::director_configuration
                                                   ::security_configuration
                                                   ::syslog_configuration
                                                   ::iaas_configuration]))

(s/def ::resource-configuration map?)

(s/def ::vmextensions-configuration (s/coll-of map?))

(s/def ::director-config (s/keys :opt-un [::az-configuration
                                          ::network-assignment
                                          ::networks-configuration
                                          ::properties-configuration
                                          ::resource-configuration
                                          ::vmextensions-configuration]))

(s/def ::config (s/keys :opt-un [::director-config]))

(defn- first-difference
  ([l r] (first-difference l r []))
  ([l r rpath]
   (match [(= l r) l r]
     [true _               _]               nil
     [_    (true :<< map?) (true :<< map?)] (some #(first-difference (% l) (% r) (conj rpath %)) (keys r))
     [_    (true :<< seq?) (true :<< seq?)] (if-not (= (count l) (count r))
                                              {:l l :r r :path rpath}
                                              (some #(first-difference %1 %2 (conj rpath %3)) l r (range)))
     :else {:l l :r r :path rpath})))

(defn print-diff
  "Print a simplified diff of the configurations to *err*"
  [deployed-config desired-config]
  (binding [*out* *err*]
    (println "Currently deployed configuration:")
    (pprint deployed-config)
    (println)
    (println "Desired configuration:")
    (pprint desired-config)
    (println)
    (if-let [{:keys [path l r]} (first-difference (util/select desired-config deployed-config)
                                                  desired-config)]
      (println (format "Found difference at %s: %s -> %s" path l r)))))

(s/fdef print-diff
        :args (s/cat :deployed-config ::config
                     :desired-config ::config)
        :ret nil?)

(defn requires-changes?
  [deployed desired]
  (not (= (util/select desired deployed) desired)))

(s/fdef requires-changes?
        :args (s/cat :deployed any? :desired any?)
        :ret boolean?)

(defn select-writable-config
  "drop keys not known to ::config spec"
  [config]
  (util/only-specd ::config config))

(s/fdef select-writable-config
        :args (s/cat :config ::config)
        :ret ::config)

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
