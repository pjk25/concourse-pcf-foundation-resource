(ns foundation-lib.foundation-configuration
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [foundation-lib.util :as util]))

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

(s/def ::product-name string?)

(s/def ::version string?)

(s/def ::endpoint string?)

(s/def ::bucket string?)

(s/def ::file string?)

(s/def ::access_key_id string?)

(s/def ::secret_access_key string?)

(s/def ::source (s/keys :req-un [::endpoint
                                 ::bucket
                                 ::file
                                 ::access_key_id
                                 ::secret_access_key]))

(s/def ::product-properties map?)

(s/def ::network-properties map?)

(s/def ::resource-config map?)

(s/def ::errand-config map?)

(s/def ::product-config (s/keys :req-un [::product-name ::version]
                                :opt-un [::source
                                         ::product-properties
                                         ::network-properties
                                         ::resource-config
                                         ::errand-config]))

(s/def ::products (s/coll-of ::product-config :distinct true :into #{}))

(s/def ::opsman-version string?)

(s/def ::deployed-config (s/keys :req-un [::opsman-version]
                                 :opt-un [::director-config ::products]))

(s/def ::desired-config (s/keys :opt-un [::opsman-version ::director-config ::products]))

(defn first-difference
  ([l r] (first-difference l r []))
  ([l r rpath]
   (match [(= l r) l r]
     [true _               _]               nil
     [_    (true :<< map?) (true :<< map?)] (some #(first-difference (% l) (% r) (conj rpath %)) (keys r))
     [_    (true :<< seq?) (true :<< seq?)] (if-not (= (count l) (count r))
                                              {:l l :r r :path rpath}
                                              (some #(first-difference %1 %2 (conj rpath %3)) l r (range)))
     :else {:l l :r r :path rpath})))

(defn requires-changes?
  [deployed desired]
  (not (= (util/select desired deployed) desired)))

(s/fdef requires-changes?
        :args (s/cat :deployed any? :desired any?)
        :ret boolean?)

(defn select-writable-config
  "drop keys not known to ::desired-config spec"
  [config]
  (util/only-specd ::desired-config config))

(s/fdef select-writable-config
        :args (s/cat :config ::desired-config)
        :ret ::desired-config)

