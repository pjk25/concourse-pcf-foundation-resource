(ns concourse-pcf-foundation-resource.cli
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.data.json :as json]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.check :as check]
            [concourse-pcf-foundation-resource.in :as in]
            [concourse-pcf-foundation-resource.out :as out]))

(set! *warn-on-reflection* true)

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread throwable]
     (println (.getMessage throwable))
     (System/exit 1))))

(def cli-options
  [["-h" "--help"]
   [nil  "--debug" "Print additional output"]])

(defn usage [options-summary]
  (->> ["Usage: concourse-pcf-foundation-resource [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        " check  Check for new versions"
        " in     Fetch a given resource"
        " out    Update a resource"]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn validate-args [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

     ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"check"} (first arguments)))
      {:action check/check :options options}

      (and (= 2 (count arguments))
           (#{"in"} (first arguments)))
      {:action in/in :options (assoc options :destination (last arguments))}

      (and (= 2 (count arguments))
           (#{"out"} (first arguments)))
      {:action out/out :options (assoc options :source (last arguments))}

      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (try
        (let [payload (json/read *in* :key-fn keyword)
              om (om-cli/->OmCli (get-in payload [:source :opsmgr]))]
          (json/write (action options om payload) *out*)
          (flush)
          (shutdown-agents))
        (catch Exception e
          (if (:debug options) (.printStackTrace e))
          (exit 1 (str "\nERROR: " e)))))))

