(ns concourse-pcf-foundation-resource.query.product-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.spec.test.alpha :as stest]
            [concourse-pcf-foundation-resource.om-cli :as om-cli]
            [concourse-pcf-foundation-resource.query.product :as product]))

(deftest state
  (stest/instrument `product/state)

  (testing "when the product is deployed at the appropriate version"
    (let [fake-om (reify om-cli/Om
                    (deployed-products [this]
                      (slurp "resources/fixtures/deployed-products/just_director.json")))]
      (is (= :deployed (product/state fake-om {:product-name "p-bosh"
                                               :version "2.5.4-build.189"
                                               :source {:pivnet-file-glob "*.pivotal"}
                                               :stemcells []})))))

  (comment (testing "when the product is deployed at the wrong version, but has been uploaded and staged at the correct one"
             (let [fake-om (reify om-cli/Om
                             (deployed-products [this]
                               (slurp "resources/fixtures/deployed-products/just_director.json")))]
               (is (= :deployed (product/state fake-om {:product-name "p-bosh"
                                                        :version "2.5.4-build.189"
                                                        :source {:pivnet-file-glob "*.pivotal"}
                                                        :stemcells []})))))))
