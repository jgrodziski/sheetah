# Sheetah

Clojure library for convenient access to Google Sheet through its Java client SDK.

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

| level l | level 2 | level 3 | level 4 |
|---------|---------|---------|---------|
| val1a   |         |         |         |
|         | val2a   |         |         |
|         | val2b   |         |         |
|         | val2c   |         |         |
|         |         | val3a   |         |
|         |         |         | val4a   |
|         |         | val3b   |         |
|         |         |         | val4b   |
|         |         |         | val4c   |

The functions `tree`, `tree-with-idx` and `treemap-with-idx` returns a tree given the columns containing the values returned by the functions in `sheetah/core` ns.
```clojure
(require [sheetah.tree :as st]')
(st/tree-with-idx (get (sheet/columns "16Q1iN4mJ_-nURLQxTpoWoYoal7YxujcdKo9tgjWjm1M" "your-sheet-name" "A2:D10") "values"))
(-> (sheet/columns "16Q1iN4mJ_-nURLQxTpoWoYoal7YxujcdKo9tgjWjm1M" "your-sheet-name" "A2:D10")
    (get "values")
    st/tree-with-idx)
    
;=> [[{:val "val1a", :row 0}
;    [{:val "val2a", :row 1}
;     {:val "val2b", :row 2}
;     [{:val "val2c", :row 3}
;      [[{:val "val3a", :row 4} [{:val "val4a", :row 5}]]
;       [{:val "val3b", :row 6}
;        [{:val "val4b", :row 7} {:val "val4c", :row 8}]]]]]]]

(-> (sheet/columns "16Q1iN4mJ_-nURLQxTpoWoYoal7YxujcdKo9tgjWjm1M" "your-sheet-name" "A2:D10")
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

## License

Copyright © 2020 Jeremie Grodziski

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
