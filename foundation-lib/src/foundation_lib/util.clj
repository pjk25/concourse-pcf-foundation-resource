(ns foundation-lib.util
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]))

(defn- unqualify
  [symbol]
  (keyword (name symbol)))

(defn only-specd
  "return the portion of x described by spec"
  [spec x]
  (let [described (if (qualified-keyword? spec) (s/describe spec) spec)]
    (match [described]
      [(['keys & r] :seq)] (let [{:keys [req req-un opt opt-un]} r]
                             (into (reduce #(if (contains? x %2)
                                              (assoc %1 %2 (only-specd %2 (%2 x)))
                                              %1)
                                           {}
                                           (concat req opt))
                                   (reduce #(let [unqualified (unqualify %2)]
                                              (if (contains? x unqualified)
                                                (assoc %1 unqualified (only-specd %2 (unqualified x)))
                                                %1))
                                           {}
                                           (concat req-un opt-un))))
      [(['coll-of i & _] :seq)] (map #(only-specd i %) x)
      :else x)))

(s/fdef only-specd
        :args (s/cat :spec (s/or :keyword qualified-keyword? :else any?)
                     :x any?)
        :ret any?)

(defn- non-empty-collection?
  [c]
  (and (or (map? c) (seq? c)) (seq c)))

(defn non-specd
  "return the remaining structure not described by spec"
  [spec x]
  (let [described (if (qualified-keyword? spec) (s/describe spec) spec)]
    (match [described]
      [(['keys & r] :seq)] (let [{:keys [req req-un opt opt-un]} r
                                 expected-keys (concat req opt (map unqualify (concat req-un opt-un)))]
                             (merge (apply dissoc x expected-keys)
                                    (reduce #(if (contains? x %2)
                                               (let [c (non-specd %2 (%2 x))]
                                                 (if (non-empty-collection? c)
                                                   (assoc %1 %2 c)
                                                   %1))
                                               %1)
                                            {}
                                            (concat req opt))
                                    (reduce #(let [unqualified (unqualify %2)]
                                               (if (contains? x unqualified)
                                                 (let [c (non-specd %2 (unqualified x))]
                                                   (if (non-empty-collection? c)
                                                     (assoc %1 unqualified c)
                                                     %1))
                                                 %1))
                                            {}
                                            (concat req-un opt-un))))
      [(['coll-of i & _] :seq)] (filter non-empty-collection? (map #(non-specd i %) x))
      ['map?] {}
      :else x)))

(defn select
  "return the portion of x that matches the structure of structure"
  [structure x]
  (match [structure x]
    [(true :<< map?) (true :<< map?)] (reduce-kv #(assoc %1 %2 (select %3 (%2 x)))
                                                 {}
                                                 structure)
    [(true :<< seq?) (true :<< seq?)] (map (partial select (first structure)) x)
    :else x))

(defn- map-values
  [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn structural-minus
  "return the portion of a not matching the structure of b"
  [a b]
  (match [a b]
    [{} {}] (merge (let [common-keys (clojure.set/intersection (set (keys a)) (set (keys b)))]
                     (reduce #(let [cm (structural-minus (%2 a) (%2 b))]
                                (cond
                                  (= ::elided cm) %1
                                  (not (empty? cm)) (assoc %1 %2 cm)
                                  :else %1))
                             {}
                             common-keys))
                   (map-values (apply dissoc a (keys b)) (constantly ::elided)))
    :else ::elided))

(s/fdef structural-minus
        :args (s/cat :a any? :b any?)
        :ret any?)
