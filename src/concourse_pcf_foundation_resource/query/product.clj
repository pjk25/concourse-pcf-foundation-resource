(ns concourse-pcf-foundation-resource.query.product
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
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
  (letfn [(matching-product? [{:keys [name version]}]
            (and (= name (:product-name product-config))
                 (string/starts-with? version (:version product-config))))]
    (cond (some matching-product? (deployed-products om)) :deployed
          (some matching-product? (staged-products om)) :staged
          (some matching-product? (available-products om)) :uploaded
          :else :none)))

(s/fdef state
        :args (s/cat :om ::om-cli/om
                     :product-config ::desired-configuration/desired-product-config)
        :ret ::state)
