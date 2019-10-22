(ns concourse-pcf-foundation-resource.query.product
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [foundation-lib.desired-configuration :as desired-configuration]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]))

(s/def ::state #{:none :uploaded :staged :deployed})

(defn- deployed-products
  [om]
  (json/read-str (om-cli/deployed-products om)
                 :key-fn keyword))

(defn- staged-products
  [om]
  (json/read-str (om-cli/staged-products om)
                 :key-fn keyword))

(defn- available-products
  [om]
  (try
    (json/read-str (om-cli/available-products om)
                   :key-fn keyword)
    (catch Exception e
      [])))

(defn state
  [om product-config]
  (cond (some #(and (= (:name %) (:product-name product-config))
                    (= (:version %) (:version product-config)))
              (deployed-products om)) :deployed
        (some #(and (= (:name %) (:product-name product-config))
                    (= (:version %) (:version product-config)))
              (staged-products om)) :staged
        (some #(and (= (:name %) (:product-name product-config))
                    (= (:version %) (:version product-config)))
              (available-products om)) :uploaded
        :else :none))

(s/fdef state
        :args (s/cat :om ::om-cli/om
                     :product-config ::desired-configuration/desired-product-config)
        :ret ::state)
