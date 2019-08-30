(ns concourse-pcf-foundation-resource.plan
  (:require [clojure.core.match :refer [match]]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [concourse-pcf-foundation-resource.foundation-configuration :as foundation]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.query.product :as product])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(s/def ::action #{:configure-director
                  :upload-product
                  :stage-product
                  :configure-product
                  :apply-changes})

(s/def ::step (s/keys :req [::action]))

(s/def ::plan (s/* ::step))

(defn- deploy-director-plan
  [desired-director-config]
  [{::action :configure-director
    ::config desired-director-config}])

(defn- deploy-product-plan
  [om desired-product-config]
  (let [configure-product-step {::action :configure-product
                                ::config desired-product-config}]
    (condp = (product/state om desired-product-config)
      :none [{::action :upload-product
              ::config desired-product-config}
             {::action :stage-product
              ::config desired-product-config}
             configure-product-step]
      :uploaded [{::action :stage-product
                  ::config desired-product-config}
                 configure-product-step]
      [configure-product-step])))

(defn- director-plan
  [deployed-director-config desired-director-config]
  (if (foundation/requires-changes? deployed-director-config desired-director-config)
    (deploy-director-plan desired-director-config)
    []))

(defn- product-plan
  [om deployed-config desired-config]
  (if (foundation/requires-changes? deployed-config desired-config)
    (deploy-product-plan om desired-config)
    []))

(defn- find-product-with-name
  [name products]
  (first (filter #(= name (:product-name %)) products)))

(defn- product-plans
  [om deployed-products desired-products]
  (let [sorted-product-names (sort (distinct (map :product-name desired-products))) ; only what's desired - no support for delete yet
        collect-product-configs-fn (fn [name] {:name name
                                               :deployed (find-product-with-name name deployed-products)
                                               :desired (find-product-with-name name desired-products)})
        product-config-pairs (map collect-product-configs-fn sorted-product-names)]
    (map #(product-plan om (:deployed %) (:desired %)) product-config-pairs)))

(defn- with-apply-changes
  [other-steps]
  (let [actions (map ::action other-steps)]
    (cond-> other-steps
      (= [:configure-director] actions)
      (concat [{::action :apply-changes
                ::options ["--skip-deploy-products"]}])

      (< 0 (count (filter #{:configure-product} actions)))
      (concat [{::action :apply-changes
                ::options (let [configure-product-steps (filter #(= :configure-product (::action %))
                                                                other-steps)
                                product-names (map #(-> % ::config :product-name) configure-product-steps)]
                            (flatten (map #(list "--product-name" %) product-names)))}]))))

(defn plan
  [om deployed-config desired-config]
  (let [ddp (director-plan (:director-config deployed-config) (:director-config desired-config))
        dpps (product-plans om (:products deployed-config) (:products desired-config))
        all-steps-except-apply-changes (apply concat ddp dpps)]
    (with-apply-changes all-steps-except-apply-changes)))

(s/fdef plan
        :args (s/cat :om ::om-cli/om
                     :deployed-config ::foundation/config
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
