(ns sheetah.core-test
  (:require [clojure.test :refer :all]
            [sheetah.core :refer :all]))

(def API_NG_SHEET_ID "16Q1iN4mJ_-nURLQxTdoWoYoal7YxuvcdKo3tgjCjm1M" )
(def DIFFUSION_API_SHEET_NAME "Diffusion-API-Endpoints")


(def columns-tree {"majorDimension" "COLUMNS",
                   "range" "test!A2:D10",
                   "values" [["val1a"] ["" "val2a" "val2b" "val2c"] ["" "" "" "" "val3a" "" "val3b"] ["" "" "" "" "" "val4a" "" "val4b" "val4c"]]})

(def rows-tree {"majorDimension" "ROWS",
                "range" "test!A2:D10",
                "values" [["val1a"] ["" "val2a"] ["" "val2b"] ["" "val2c"] ["" "" "val3a"] ["" "" "" "val4a"] ["" "" "val3b"] ["" "" "" "val4b"] ["" "" "" "val4c"]]})

(def columns-2darray {"majorDimension" "COLUMNS",
                      "range" "test!E2:H10",
                      "values" [["TRUE" "FALSE" "TRUE" "TRUE" "TRUE" "TRUE" "FALSE" "TRUE" "TRUE"] ["string" "string" "integer" "integer" "string" "string" "boolean" "boolean" "string"] ["val1, val2, val3" "val1 " "val2" "val1, val2 " "val1, val2, val3" "val1, val2, val3" "val1, val2, val3" "val1, val2, val3"]]})

(def rows-2darray {"majorDimension" "ROWS",
                   "range" "test!E2:H10",
                   "values" [["TRUE" "string" "val1, val2, val3"] ["FALSE" "string" "val1 "] ["TRUE" "integer" "val2"] ["TRUE" "integer" "val1, val2 "] ["TRUE" "string" "val1, val2, val3"] ["TRUE" "string" "val1, val2, val3"] ["FALSE" "boolean" "val1, val2, val3"] ["TRUE" "boolean" "val1, val2, val3"] ["TRUE" "string"]]})

(deftest retrieve-rows-and-columns-test
  (testing ""
    ))
