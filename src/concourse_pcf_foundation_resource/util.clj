(ns concourse-pcf-foundation-resource.util)

(defn keywordize [parsed-json]
  (if (map? parsed-json)
    (zipmap (map keyword (keys parsed-json)) 
            (map keywordize (vals parsed-json)))
    parsed-json))
