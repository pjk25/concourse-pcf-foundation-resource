(ns concourse-pcf-foundation-resource.util
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]))

(defn only-specd
  [spec x]
  (match [(s/describe spec)]
         [(['keys & r] :seq)] (let [{:keys [req req-un opt opt-un]} r]
                                (into (reduce #(assoc %1 %2 (only-specd %2 (%2 x)))
                                              {}
                                              (concat req opt))
                                      (reduce #(let [unqualified (keyword (name %2))]
                                                 (assoc %1 unqualified (only-specd %2 (unqualified x))))
                                              {}
                                              (concat req-un opt-un))))
         :else x))

(s/fdef only-spec
        :args (s/cat :spec qualified-keyword?
                     :x any?)
        :ret any?)
