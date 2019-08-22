(ns concourse-pcf-foundation-resource.query.product
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]))

(s/def ::state #{:none :deployed :staged :uploaded})

(defn state
  [om product-config]
  (let [deployed-products (json/read-str (om-cli/deployed-products om))]
    nil))

(s/fdef state
  :args (s/cat :om ::om-cli/om
               :product-config ::foundation/product-config)
  :ret ::state)
