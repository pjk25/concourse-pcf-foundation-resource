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

(defn plan
  [deployed-config desired-config]
  (let [equal-dc (= (:director-config deployed-config)
                    (:director-config desired-config))]
    (match [deployed-config desired-config equal-dc]
           [{:director-config _} {:director-config _}  _] nil
           [_                    {:director-config dc} _] (deploy-director-plan dc)
           :else nil)))

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
