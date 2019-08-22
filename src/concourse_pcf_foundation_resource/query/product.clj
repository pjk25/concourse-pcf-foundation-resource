(ns concourse-pcf-foundation-resource.query.product
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]))

(s/def ::state #{:none :deployed :staged :uploaded})

(defn state
  [om product-config]
  (let [deployed-products (json/read-str (om-cli/deployed-products om) :key-fn keyword)]
    (cond (some #(and (= (:name %) (:product-name product-config))
                      (= (:version %) (:version product-config)))
                (map #(select-keys % [:name :version]) deployed-products)) :deployed
          :else :none)))

(s/fdef state
        :args (s/cat :om ::om-cli/om
                     :product-config ::foundation/product-config)
        :ret ::state)
