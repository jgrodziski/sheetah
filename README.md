# Sheetah

Clojure library for convenient access to Google Sheet through its Java client SDK.

## Installation

### deps.edn

```clojure
{sheetah {:git/url "git@github.com:jgrodziski/sheetah.git" 
          :sha "c8acd8df7a7a290c18fb7c24c0a5d36a1092fd7a"}}
```

## Usage

### Credentials

Go to the [Java Quickstart page](https://developers.google.com/sheets/api/quickstart/java) and click on the "Enable the Google Sheets API" button.
You should get a `credentials.json` file that you can put in the `resources` directory or any classpath related dir for the lib to load it later.

### Read values from the google sheets

You need to retrieve the identifier of your sheet, the URL contains it, and put it as first arg to the functions.

```clojure

(require '[sheetah.core :as sheet])
(sheet/columns "16Q1iN4mJ_-nURLQxTpoWoYoal7YxujcdKo9tgjWjm1M" "your-sheet-name" "B2:E17" )
;; you could also use a named range
(sheet/rows "16Q1iN4mJ_-nURLQxTpoWoYoal7YxujcdKo9tgjWjm1M" "your-sheet-name" "my-named-range" )

```

### Transform values as a tree

2d array are easy to get but a tree structure is more interesting

Given a structure like the following in the sheet:

|    | level 1 | level 2 | level 3 | level 4 |
|----|---------|---------|---------|---------|
|  2 | `val1a` |         |         |         |
|  3 |         | `val2a` |         |         |
|  4 |         | `val2b` |         |         |
|  5 |         | `val2c` |         |         |
|  6 |         |         | `val3a` |         |
|  7 |         |         |         | `val4a` |
|  8 |         |         | `val3b` |         |
|  9 |         |         |         | `val4b` |
| 10 |         |         |         | `val4c` |

The functions `tree`, `tree-with-idx` and `treemap-with-idx` returns a tree given the columns containing the values returned by the functions in `sheetah/core` ns.
```clojure
(require [sheetah.tree :as st]')

(def SHEET_ID "16Q1iN4mJ_-nURLQxTpoWoYoal7YxujcdKo9tgjWjm1M" )

(-> (sheet/columns SHEET_ID "your-sheet-name" "A2:D10")
    (get "values")
    st/tree)
; tree function doesn't returns the row index so it's inconvenient if we want to retrieve data in a 2d array on the right of the tree
;=> ["val1a"
;      ["val2a" "val2b" "val2c" 
;         ["val3a" 
;             ["val4a"]
;          "val3b" 
;             ["val4b" "val4c"]]]]


(-> (sheet/columns SHEET_ID "your-sheet-name" "A2:D10")
    (get "values")
    st/tree-with-idx)
;=> [[{:val "val1a", :row 0}
;    [{:val "val2a", :row 1}
;     {:val "val2b", :row 2}
;     [{:val "val2c", :row 3}
;      [[{:val "val3a", :row 4} [{:val "val4a", :row 5}]]
;       [{:val "val3b", :row 6}
;        [{:val "val4b", :row 7} {:val "val4c", :row 8}]]]]]]]

(-> (sheet/columns SHEET_ID "your-sheet-name" "A2:D10")
    (get "values")
    st/tree-with-idx
    st/treemap-with-idx)
;=> {"val1a"
;    [0
;     {"val2a" [1 nil],
;      "val2b" [2 nil],
;      "val2c" [3 {"val3a" [4 {"val4a" [5 nil]}],
;                  "val3b" [6 {"val4b" [7 nil], 
;                              "val4c" [8 nil]}]}]}]}

    
```

### tabular and tree data

Consider the following tabular and tree data in the google sheet :

|     | level 1 | level 2 | level 3 | level 4 | details-col1 | details-col2 | details-col3     | details-col4               |
| :-- | :--     | :--     | :--     | :--     | :--          | :--          | :--              | :--                        |
|   2 | val1a   |         |         |         | TRUE         | string       | val1, val2, val3 | val1a is a super thing     |
|   3 |         | val2a   |         |         | FALSE        | string       | val1             | val2a attribute is awesome |
|   4 |         | val2b   |         |         | TRUE         | integer      | val2             | nothing to say             |
|   5 |         | val2c   |         |         | TRUE         | integer      | val1, val2       | Humm                       |
|   6 |         |         | val3a   |         | TRUE         | object       | val1, val2, val3 | Ho                         |
|   7 |         |         |         | val4a   | TRUE         | string       | val1, val2, val3 | Ha                         |
|   8 |         |         | val3b   |         | FALSE        | object       | val1, val2, val3 | Hey                        |
|   9 |         |         |         | val4b   | TRUE         | string       | val1, val2, val3 | Yo                         |
|  10 |         |         |         | val4c   | TRUE         | boolean      |                  |                            |

You can associate tabular data with the tree (see previous section) with the following code:
```clojure

(require '[sheetah.core :as sheet])
(require '[sheetah.table :as table])
(require '[sheetah.tree :as tree])

(def SHEET_ID "16Q1iN4mJ_-nURLQxTpoWoYoal7YxujcdKo9tgjWjm1M" )
(def tree (-> (sheet/columns SHEET_ID "your-sheet-name" "A2:D10")
              (get "values")
              st/tree-with-idx
              st/treemap-with-idx))
(def table (-> (sheet/rows  SHEET_ID "your-sheet-name" "E2:H10")
               (get "values")))

(tree/assoc-table-rows tree table)
;;=> 
;; {"val1a"
;;  [["TRUE" "string" "val1, val2, val3"]
;;   {"val2a" [["FALSE" "string" "val1 "] nil],
;;    "val2b" [["TRUE" "integer" "val2"] nil],
;;    "val2c"  [["TRUE" "integer" "val1, val2 "]
;;              {"val3a" [["TRUE" "string" "val1, val2, val3"]
;;                        {"val4a" [["TRUE" "string" "val1, val2, val3"] nil]}],
;;               "val3b" [["FALSE" "boolean" "val1, val2, val3"]
;;                        {"val4b" [["TRUE" "boolean" "val1, val2, val3"] nil],
;;                         "val4c" [["TRUE" "string"] nil]}]}]}]}

(tree/assoc-table-rows tree (normalize table [{:name :details-col1 :fn (fn [x] (if (= x "TRUE") true false))}
                                              {:name :details-col2 :fn (fn [x] (if (#{ "string" "integer" "object" "boolean"} x) x "string"))}
                                              {:name :details-col3}
                                              {:name :details-col4}]))

```

### Normalize the data found in the table

The values found in table (vector of vector) may needs some process before being handled because the google sheet only returns String.

```clojure

(def table-data (sheet/rows SHEET_ID "your-sheet-name" "E2:H10"))
;; => Google sheets only returns String value (even for numbers) and omits the empty cells so a normalize function is useful
;; [["TRUE"  "string"  "val1, val2, val3"]
;;  ["FALSE" "string"  "val1 "]
;;  ["TRUE"  "integer" "val2"]
;;  ["TRUE"  "integer" "val1, val2 "]
;;  ["TRUE"  "string"  "val1, val2, val3"]
;;  ["TRUE"  "string"  "val1, val2, val3"]
;;  ["FALSE" "boolean" "val1, val2, val3"]
;;  ["TRUE"  "boolean" "val1, val2, val3"]
;;  ["TRUE"  "string"]]

(normalize table-data [{:name :detail-1 :fn (fn [x] (if (= x "TRUE") true false))}
                       {:name :detail-2 :fn (fn [x] (if (#{ "string" "integer" "object" "boolean"} x) x "string"))}
                       {:name :detail-3}
                       {:name :detail-4}])
;; => 
;; ({:detail-1 true,  :detail-2 "string",  :detail-3 "val1, val2, val3", :detail-4 ""}
;;  {:detail-1 false, :detail-2 "string",  :detail-3 "val1",             :detail-4 ""}
;;  {:detail-1 true,  :detail-2 "integer", :detail-3 "val2",             :detail-4 ""}
;;  {:detail-1 true,  :detail-2 "integer", :detail-3 "val1, val2",       :detail-4 ""}
;;  {:detail-1 true,  :detail-2 "string",  :detail-3 "val1, val2, val3", :detail-4 ""}
;;  {:detail-1 true,  :detail-2 "string",  :detail-3 "val1, val2, val3", :detail-4 ""}
;;  {:detail-1 false, :detail-2 "boolean", :detail-3 "val1, val2, val3", :detail-4 ""}
;;  {:detail-1 true,  :detail-2 "boolean", :detail-3 "val1, val2, val3", :detail-4 ""}
;;  {:detail-1 true,  :detail-2 "string",  :detail-3 "",                 :detail-4 ""})
                                                   
```

## License

Copyright © 2020 Jeremie Grodziski

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
