(ns concourse-pcf-foundation-resource.query.pending-changes
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]))

(defn interpret
  [parsed-pending-changes-response]
  (match [parsed-pending-changes-response]
    [{:product_changes [{:action "install" :staged {:identifier "p-bosh"}}]}] :fresh-opsman
    [(_ :guard (fn [resp] (some #(not (= "unchanged" (:action %))) (:product_changes resp))))] :yes
    :else :no))

(s/fdef interpret
        :args (s/cat :parsed-pending-changes-response map?)
        :ret #{:fresh-opsman :yes :no})
