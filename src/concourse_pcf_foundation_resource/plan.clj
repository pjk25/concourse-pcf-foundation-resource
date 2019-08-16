(ns concourse-pcf-foundation-resource.plan
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.om-cli :as om-cli])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(s/def ::action #{:configure-director :stage-product :configure-product :apply-changes})

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
  [;; a first step of uploading the product is probably necessary (om upload-product)
   {::action :stage-product
    ::config desired-product-config}
   {::action :configure-product
    ::config desired-product-config}
   {::action :apply-changes
    ::options ["--product-name" (:product-name desired-product-config)]}])

(defn- find-product-with-name
  [name products]
  (first (filter #(= name (:product-name %)) products)))

(defmulti virtual-apply-step (fn [step deployed-config] (::action step)))

(defmethod virtual-apply-step :configure-director [step deployed-config]
  (assoc deployed-config :director-config (::config step)))

(defmethod virtual-apply-step :stage-product [step deployed-config]
  deployed-config)

(defmethod virtual-apply-step :configure-product [step deployed-config]
  (let [name (:product-name (::config step))
        product-index (first (keep-indexed #(if (= name (:product-name %2)) %1) (:products deployed-config)))]
    (assoc-in deployed-config [:products product-index] (::config step))))

(defmethod virtual-apply-step :apply-changes [step deployed-config]
  deployed-config)

(defn- virtual-apply-plan
  [plan deployed-config]
  ;; this could be made more complex where staged changes are modeled, and applying moves them over to "deployed"
  (reduce #(virtual-apply-step %2 %1) deployed-config plan))

(defn- plans
  [deployed-config desired-config]
  (lazy-seq
   (if (foundation/requires-changes? (:director-config deployed-config) (:director-config desired-config))
     (let [ddp (deploy-director-plan (:director-config desired-config))]
       (cons ddp (plans (virtual-apply-plan ddp deployed-config) desired-config)))
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
         (let [dpp (deploy-product-plan desired)]
           (cons dpp (plans (virtual-apply-plan dpp deployed-config) desired-config)))
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
  (first (filter valid-plan? (take 1000 (plans deployed-config desired-config)))))

(s/fdef plan
        :args (s/cat :deployed-config ::foundation/config
                     :desired-config ::foundation/config)
        :ret ::plan)

; TODO: make this multiple arity like virtual-apply-plan
(defmulti executor ::action)

(defmethod executor :configure-director [step]
  (fn [cli-options om]
    (om-cli/configure-director om (::config step))))

(defmethod executor :configure-product [step]
  (fn [cli-options om]
    (om-cli/configure-product om (::config step))))

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

(defmethod description :stage-product [step]
  (str (::action step) "-" "Stage" (:product-name (::config step)) (:version ())))

(defmethod description :configure-product [step]
  (str (::action step) "-" "Configure" (:product-name (::config step))))

(defmethod description :apply-changes [step]
  (str (::action step) " - " "Apply Changes"))

(defn describe-plan
  [p]
  (string/join "\n" (map #(format "  %d. %s" (inc %2) (description %1)) p (range))))

(s/fdef describe-plan
        :args (s/cat :plan ::plan)
        :ret string?)
