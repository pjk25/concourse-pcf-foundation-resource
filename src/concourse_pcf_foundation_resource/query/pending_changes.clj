(ns concourse-pcf-foundation-resource.query.pending-changes
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]))

(defn- fresh-opsman?
  [parsed-pending-changes-response]
  (let [{:keys [product_changes]} parsed-pending-changes-response]
    (and (= 1 (count product_changes))
         (= "install" (:action (first product_changes)))
         (= "p-bosh" (:identifier (:staged (first product_changes)))))))

(defn interpret
  [parsed-pending-changes-response]
  (match [parsed-pending-changes-response]
         [(true :<< fresh-opsman?)] :fresh-opsman
         :else :no))

(s/fdef interpret
        :args (s/cat :parsed-pending-changes-response map?)
        :ret #{:fresh-opsman :yes :no})
