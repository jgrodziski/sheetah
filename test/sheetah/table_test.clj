(ns sheetah.table-test
  (:require [sheetah.table :refer :all]
            [sheetah.core-test :refer [columns-table columns-tree rows-table rows-tree]]
            [clojure.test :refer :all]))

(def cols-structure  [{:name :detail-2 :fn (fn [x] (if (= x "TRUE") true false))}
                      {:name :detail-1 :fn (fn [x] (if (#{ "string" "integer" "object" "boolean"} x) x "string"))}
                      {:name :detail-3}
                      {:name :detail-4}])

(deftest normalize-table-data-test
  (testing "normalize"
    (is (= (normalize (get rows-table "values") cols-structure)
           [{:detail-2 true, :detail-1 "string", :detail-3 "val1, val2, val3", :detail-4 "val1a is a super thing"}
            {:detail-2 false, :detail-1 "string", :detail-3 "val1", :detail-4 "val2a attribute is awesome"}
            {:detail-2 true, :detail-1 "integer", :detail-3 "val2", :detail-4 "nothing to say"}
            {:detail-2 true, :detail-1 "integer", :detail-3 "val1, val2", :detail-4 "Humm"}
            {:detail-2 true, :detail-1 "object", :detail-3 "val1, val2, val3", :detail-4 "Ho"}
            {:detail-2 true, :detail-1 "string", :detail-3 "val1, val2, val3", :detail-4 "Ha"}
            {:detail-2 false, :detail-1 "object", :detail-3 "val1, val2, val3", :detail-4 "Hey"}
            {:detail-2 true, :detail-1 "string", :detail-3 "val1, val2, val3", :detail-4 "Yo"}
            {:detail-2 true, :detail-1 "boolean", :detail-3 "", :detail-4 "1"}]))))
