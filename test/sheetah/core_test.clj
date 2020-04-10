(ns sheetah.core-test
  (:require [clojure.test :refer :all]
            [sheetah.core :refer :all]))

(def API_NG_SHEET_ID "16Q1iN4mJ_-nURLQxTdoWoYoal7YxuvcdKo3tgjCjm1M" )
(def DIFFUSION_API_SHEET_NAME "Diffusion-API-Endpoints")

(def columns-tree
  {"majorDimension" "COLUMNS", "range" "test!A2:D10", "values" [["val1a"] ["" "val2a" "val2b" "val2c"] ["" "" "" "" "val3a" "" "val3b"] ["" "" "" "" "" "val4a" "" "val4b" "val4c"]]})

(def rows-tree {"majorDimension" "ROWS", "range" "test!A2:D10", "values" [["val1a"] ["" "val2a"] ["" "val2b"] ["" "val2c"] ["" "" "val3a"] ["" "" "" "val4a"] ["" "" "val3b"] ["" "" "" "val4b"] ["" "" "" "val4c"]]})

;;
(def columns-table {"majorDimension" "COLUMNS", "range" "test!E2:H10", "values" [["TRUE" "FALSE" "TRUE" "TRUE" "TRUE" "TRUE" "FALSE" "TRUE" "TRUE"] ["string" "string" "integer" "integer" "object" "string" "object" "string" "boolean"] ["val1, val2, val3" "val1 " "val2" "val1, val2 " "val1, val2, val3" "val1, val2, val3" "val1, val2, val3" "val1, val2, val3"] ["val1a is a super thing" "val2a attribute is awesome" "nothing to say" "Humm" "Ho" "Ha" "Hey" "Yo" "1"]]})

(def rows-table {"majorDimension" "ROWS", "range" "test!E2:H10", "values" [["TRUE" "string" "val1, val2, val3" "val1a is a super thing"] ["FALSE" "string" "val1 " "val2a attribute is awesome"] ["TRUE" "integer" "val2" "nothing to say"] ["TRUE" "integer" "val1, val2 " "Humm"] ["TRUE" "object" "val1, val2, val3" "Ho"] ["TRUE" "string" "val1, val2, val3" "Ha"] ["FALSE" "object" "val1, val2, val3" "Hey"] ["TRUE" "string" "val1, val2, val3" "Yo"] ["TRUE" "boolean" "" "1"]]})

