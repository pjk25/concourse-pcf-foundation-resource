(ns concourse-pcf-foundation-resource.check
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]))

(s/def ::source (s/keys :req-un [::opsmgr]))

(s/def ::version string?)

(s/def ::check-payload (s/keys :req-un [::source ::version]))

(defn check
  [om check-payload]
  (let [{:keys [source]} check-payload
        {:keys [opsmgr]} source]
    (om opsmgr "staged-director-config")))

(s/fdef check
        :args (s/cat :om (s/fspec :args ::om-cli/om-args)
                     :check-payload ::check-payload)
        :ret ::version)

; approximately, this should:
; 1. check if changes are being applied, if so fail (GET /api/v0/installations)
; 2. check if changes are pending, if so fail (GET /api/v0/staged/pending_changes)
; 3. fetch the staged director config, saving it to a file
; 4. compute the sha1sum of the contents of that file
; 5. return that sha as the version of the foundation
