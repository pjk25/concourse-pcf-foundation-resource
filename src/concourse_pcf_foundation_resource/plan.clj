(ns concourse-pcf-foundation-resource.plan
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.om-cli :as om-cli])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(s/def ::action #{:configure-director :apply-changes})

(s/def ::step (s/keys :req [::action]))

(s/def ::plan (s/* ::step))

(defn- deploy-director-plan
  [desired-director-config]
  [{::action :configure-director
    ::config desired-director-config}
   {::action :apply-changes
    ::options ["--skip-deploy-products"]}])

(defn- deploy-product-plan
  [desired-product-config]
  [{::action :configure-product
    ::config desired-product-config}
   {::action :apply-changes
    ::options ["--product-name" (:product-name desired-product-config)]}])

(defn- find-product-with-name
  [name products]
  (first (filter #(= name (:product-name %)) products)))

(defn- replacing-product-with-name
  [name deployed-config desired-config]
  (let [product-index (first (keep-indexed #(if (= name (:product-name %2)) %1) (:products deployed-config)))]
    (assoc-in deployed-config [:products product-index] (find-product-with-name name (:products desired-config)))))

(comment (defn- virtual-apply-plan
           [plan deployed-config]
           nil))

(defn- plans
  [deployed-config desired-config]
  (lazy-seq
   (if (foundation/requires-changes? (:director-config deployed-config) (:director-config desired-config))
     (cons (deploy-director-plan (:director-config desired-config))
           (plans (assoc deployed-config :director-config (:director-config desired-config)) desired-config))
     (let [deployed-products (:products deployed-config)
           desired-products (:products desired-config)
           sorted-product-names (sort (distinct (map :product-name desired-products))) ; only what's desired - no support for delete yet
           collect-product-configs-fn (fn [name] {:name name
                                                  :deployed (find-product-with-name name deployed-products)
                                                  :desired (find-product-with-name name desired-products)})
           product-config-pairs (map collect-product-configs-fn sorted-product-names)
           has-delta? (fn [{:keys [deployed desired]}]
                        (if desired (foundation/requires-changes? deployed desired)))]
       (if-let [{:keys [name deployed desired]} (first (filter has-delta? product-config-pairs))]
         (cons (deploy-product-plan desired)
               (plans (replacing-product-with-name name deployed desired) desired-config))
         (list []))))))

; The plan is valid if after it is applied, all versions satisfy the versioning contstraints
; The versioning constraints should be an edn file given as an input to the 'put' of the resource
; generally speaking, it will be along the lines of, if necessary, deploy director, then other products
; the versioning constraints should more of less enforce that PAS goes first
(defn- valid-plan?
  [plan]
  true)

(defn plan
  [deployed-config desired-config]
  ;; if the count of take 1000 plans = 1000, it appears we cannot converge, so we should bail, even before we filter the valid plans
  (first (filter valid-plan? (plans deployed-config desired-config))))

(s/fdef plan
        :args (s/cat :deployed-config ::foundation/config
                     :desired-config ::foundation/config)
        :ret ::plan)

(defmulti executor ::action)

(defmethod executor :configure-director [step]
  (fn [cli-options om]
    (om-cli/configure-director om (::config step))))

(defmethod executor :apply-changes [step]
  (fn [cli-options om]
    (om-cli/apply-changes om (::options step))))

(s/fdef executor
        :args (s/cat :step ::step)
        :ret (s/fspec :args (s/cat :cli-options map?
                                   :om ::om-cli-om)
                      :ret nil?))

(defmulti description ::action)

(defmethod description :configure-director [step]
  (str (::action step) " - " "Configure the director tile"))

(defmethod description :apply-changes [step]
  (str (::action step) " - " "Apply Changes"))

(defn describe-plan
  [p]
  (string/join "\n" (map #(format "  %d. %s" (inc %2) (description %1)) p (range))))

(s/fdef describe-plan
        :args (s/cat :plan ::plan)
        :ret string?)
