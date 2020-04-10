(ns sheetah.tree-test
  (:require [sheetah.tree :refer :all]
            [sheetah.core :as core]
            [sheetah.core-test :refer [columns-2darray columns-tree rows-2darray rows-tree]]
            [sheetah.2darray :as tab]
            [clojure.test :refer :all]))

(def attribute-details  [{:name :detail-2 :fn (fn [x] (if (= x "TRUE") true false))}
                         {:name :detail-1 :fn (fn [x] (if (#{ "string" "integer" "object" "boolean"} x) x "string"))}
                         {:name :detail-3}
                         {:name :detail-4}])


(deftest tree-test
  (testing ""))
