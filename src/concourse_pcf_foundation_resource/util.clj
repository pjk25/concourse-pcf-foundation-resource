(ns concourse-pcf-foundation-resource.util
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]))

(defn only-specd
  [spec x]
  (let [described (if (qualified-keyword? spec) (s/describe spec) spec)]
    (match [described]
           [(['keys & r] :seq)] (let [{:keys [req req-un opt opt-un]} r]
                                  (into (reduce #(if (contains? x %2)
                                                   (assoc %1 %2 (only-specd %2 (%2 x)))
                                                   %1)
                                                {}
                                                (concat req opt))
                                        (reduce #(let [unqualified (keyword (name %2))]
                                                   (if (contains? x unqualified)
                                                     (assoc %1 unqualified (only-specd %2 (unqualified x)))
                                                     %1))
                                                {}
                                                (concat req-un opt-un))))
           [(['coll-of i & _] :seq)] (mapv #(only-specd i %) x)
           :else x)))

(s/fdef only-specd
        :args (s/cat :spec (s/or :keyword qualified-keyword? :else any?)
                     :x any?)
        :ret any?)
