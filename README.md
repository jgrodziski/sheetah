# Sheety

Clojure library for convenient access to Google Sheet through its Java client SDK.

## Usage

### Credentials

Go to the [Java Quickstart page](https://developers.google.com/sheets/api/quickstart/java) and click on the "Enable the Google Sheets API" button.
You should get a `credentials.json` file that you can put in the `resources` directory or any classpath related dir for the lib to load it later.

### Read values from the google sheets

You need to retrieve the identifier of your sheet, the URL contains it, and put it as first arg to the functions.

```clojure

(require '[sheety.core :as sheet])
(sheet/columns "16Q1iN4mJ_-nURLQxTpoWoYoal7YxujcdKo9tgjWjm1M" "your-sheet-name" "B2:E17" )
;; you could also use a named range
(sheet/rows "16Q1iN4mJ_-nURLQxTpoWoYoal7YxujcdKo9tgjWjm1M" "your-sheet-name" "my-named-range" )

```

## License

Copyright © 2020 Jeremie Grodziski

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
