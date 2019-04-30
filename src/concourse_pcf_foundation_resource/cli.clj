(ns concourse-pcf-foundation-resource.cli
  (:gen-class))

(set! *warn-on-reflection* true)

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread throwable]
     (println (.getMessage throwable))
     (System/exit 1))))

(defn -main [& args]
  (println "foo"))