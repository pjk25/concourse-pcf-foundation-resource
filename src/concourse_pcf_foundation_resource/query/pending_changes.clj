(ns concourse-pcf-foundation-resource.query.pending-changes
  (:require [clojure.spec.alpha :as s]))

(defn changes-pending?
  [parsed-pending-changes-response]
  false)

(s/fdef changes-pending?
        :args (s/cat :parsed-pending-changes-response map?)
        :ret boolean?)

(defn fresh-opsman?
  [parsed-pending-changes-response]
  (let [{:keys [product_changes]} parsed-pending-changes-response]
    (and (= 1 (count product_changes))
         (= "install" (:action (first product_changes)))
         (= "p-bosh" (:identifier (:staged (first product_changes)))))))

(s/fdef fresh-opsman?
        :args (s/cat :parsed-pending-changes-response map?)
        :ret boolean?)
