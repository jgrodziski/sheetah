(ns sheetah.tree-test
  (:require [sheetah.tree :as st :refer :all]
            [sheetah.core :as core]
            [sheetah.core-test :refer [columns-table columns-tree rows-table rows-tree]]
            [sheetah.table :as table]

            [clojure.test :refer :all]))



(deftest tree-test
  (testing "A simple vector tree"
    (is (= (-> columns-tree
               (get "values")
               st/tree)
           ["val1a"
            ["val2a" "val2b" "val2c" ["val3a" ["val4a"] "val3b" ["val4b" "val4c"]]]])))
  (testing "A vector tree with index"
    (is (= (-> columns-tree
               (get "values")
               st/tree-with-idx)
           [[{:val "val1a", :row 0}
             [{:val "val2a", :row 1}
              {:val "val2b", :row 2}
              [{:val "val2c", :row 3}
               [[{:val "val3a", :row 4} [{:val "val4a", :row 5}]]
                [{:val "val3b", :row 6}
                 [{:val "val4b", :row 7} {:val "val4c", :row 8}]]]]]]]
           )))
  (testing "A map tree with index"
    (is (= (-> columns-tree
               (get "values")
               st/tree-with-idx
               st/treemap-with-idx)
           {"val1a"
            [0
             {"val2a" [1 nil],
              "val2b" [2 nil],
              "val2c"
              [3
               {"val3a" [4 {"val4a" [5 nil]}],
                "val3b" [6 {"val4b" [7 nil], "val4c" [8 nil]}]}]}]}))))

(deftest tree-with-table-data-test
  (testing "A tree with table data associated"
    (is (= (assoc-table-rows (-> columns-tree
                                 (get "values")
                                 st/tree-with-idx
                                 st/treemap-with-idx) (get rows-table "values"))
           {"val1a"
            [["TRUE" "string" "val1, val2, val3" "val1a is a super thing"]
             {"val2a" [["FALSE" "string" "val1 " "val2a attribute is awesome"] nil],
              "val2b" [["TRUE" "integer" "val2" "nothing to say"] nil],
              "val2c"
              [["TRUE" "integer" "val1, val2 " "Humm"]
               {"val3a"
                [["TRUE" "object" "val1, val2, val3" "Ho"]
                 {"val4a" [["TRUE" "string" "val1, val2, val3" "Ha"] nil]}],
                "val3b"
                [["FALSE" "object" "val1, val2, val3" "Hey"]
                 {"val4b" [["TRUE" "string" "val1, val2, val3" "Yo"] nil],
                  "val4c" [["TRUE" "boolean" "" "1"] nil]}]}]}]}))))
