(ns concourse-pcf-foundation-resource.check
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]))

(s/def ::version string?)

(defn check
  [om previous-version]
  (let [installations-result (om-cli/curl om "/api/v0/installations")]
    (let [pending-changes-result (om-cli/curl om "/api/v0/staged/pending_changes")]
      (let [config-result (om-cli/staged-director-config om)]
        "foo"))))

(s/fdef check
        :args (s/cat :om ::om-cli/om
                     :previous-version ::version)
        :ret ::version)

; approximately, this should:
; 1. check if changes are being applied, if so fail (GET /api/v0/installations)
; 2. check if changes are pending, if so fail (GET /api/v0/staged/pending_changes)
; 3. fetch the staged director config, saving it to a file
; 4. compute the sha1sum of the contents of that file
; 5. return that sha as the version of the foundation
