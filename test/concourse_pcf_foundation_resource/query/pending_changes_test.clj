(ns concourse-pcf-foundation-resource.query.pending-changes-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [clojure.data.json :as json]
            [concourse-pcf-foundation-resource.query.pending-changes :as pending-changes]))

(deftest interpret
  (stest/instrument `pending-changes/interpret)

  (testing "when opsman is configured with auth, but nothing has been deployed"
    (is (= :fresh-opsman (pending-changes/interpret (json/read-str (slurp "resources/fixtures/curl/pending_changes/fresh_opsman.json")
                                                                   :key-fn keyword)))))

  (testing "when the director has been finished installing/deploying and nothing else is queued"
    (is (= :no (pending-changes/interpret (json/read-str (slurp "resources/fixtures/curl/pending_changes/director_deployed.json")
                                                         :key-fn keyword))))))
