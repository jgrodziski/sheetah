(ns sheetah.core
  (:require [clojure.java.io :as io])
  (:import com.google.api.client.auth.oauth2.Credential
           com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
           com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver$Builder
           com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow$Builder
           com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
           com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
           com.google.api.client.http.javanet.NetHttpTransport
           com.google.api.client.json.JsonFactory
           com.google.api.client.json.jackson2.JacksonFactory
           com.google.api.client.util.store.FileDataStoreFactory
           com.google.api.services.sheets.v4.Sheets$Builder
           com.google.api.services.sheets.v4.SheetsScopes
           com.google.api.services.sheets.v4.model.ValueRange))

(def JSON_FACTORY (JacksonFactory/getDefaultInstance))
(def FILE_DATA_STORE_FACTORY (FileDataStoreFactory. (io/file "/tmp/tokens")))
(def APPLICATION_NAME "sheetah-v0.0.1")

(defn credentials
  ([transport]
   (credentials transport "credentials.json"))
  ([transport creds]
   (with-open [rdr (io/reader (io/resource creds))]
     (let [client-secrets (GoogleClientSecrets/load JSON_FACTORY rdr)
           flow (-> (GoogleAuthorizationCodeFlow$Builder. transport JSON_FACTORY client-secrets (java.util.ArrayList. [SheetsScopes/SPREADSHEETS]))
                    (.setDataStoreFactory FILE_DATA_STORE_FACTORY)
                    (.setAccessType "offline")
                    (.build))
           receiver (-> (LocalServerReceiver$Builder.) (.setPort 8888) (.build))]
       (-> (AuthorizationCodeInstalledApp. flow receiver) (.authorize "user"))))))

(defn sheets
  ^com.google.api.services.sheets.v4.Sheets []
  (let [transport (GoogleNetHttpTransport/newTrustedTransport)
        sheets (-> (Sheets$Builder. transport JSON_FACTORY (credentials transport))
                   (.setApplicationName APPLICATION_NAME)
                   (.build))]
    sheets))

(defn alpha-uppercase? [s]
  (re-matches #"[A-Z]+" s))

(defn assert-row [x]
  (if (instance? java.lang.String x)
    (try (java.lang.Long/valueOf x) (catch NumberFormatException nfe (throw (ex-info "A row reference of the sheet in a string must be convert correctly to a long or integer" {:row x}))))
    (when (not (or (instance? java.lang.Integer x) (instance? java.lang.Long x)))
      (throw (ex-info "A row reference of the sheet must be an Integer or a long" {:row x})))))

(defn cell-value
  "return the value in a unique cell denoted by its column and row reference, e.g.: \"A\" and \"1\" for the A1 cell in the sheet"
  [sheet-id sheet-name column row]
  (when (not (alpha-uppercase? column)) (throw (ex-info "Column must be a string alpha with only uppercase character" {:column column})))
  (assert-row row)
  (-> (sheets)
      (.spreadsheets)
      (.values)
      (.get sheet-id (str sheet-name "!" column row))
      (.execute)
      (.get "values")
      (ffirst)))

(defn named-ranges
  "Return all the named ranges of the sheet, return a map with key a string of the name of the range and value of the range like {:endColumnIndex 15, :endRowIndex 237, :startColumnIndex 6, :startRowIndex 8} "
  [sheet-id]
  (let [raw-named-ranges (-> (sheets)
                             (.spreadsheets)
                             (.get sheet-id)
                             (.execute)
                             (.get "namedRanges"))]
    (into {} (map (fn [{:strs [name range] :as namedRange}] [name (into {} (map (fn [[k v]] [(keyword k) v])) range)]) raw-named-ranges))))

(defn cells-values
  [sheet-id sheet-name range major-dim]
  (-> (sheets)
      (.spreadsheets)
      (.values)
      (.get sheet-id (str sheet-name "!" range))
      (.setMajorDimension major-dim)
      (.execute)))

(defn columns [sheet-id sheet-name range] (cells-values sheet-id sheet-name range "COLUMNS"))
(defn rows    [sheet-id sheet-name range] (cells-values sheet-id sheet-name range "ROWS"))

(defn- write-cells-values [sheet-id sheet-name range major-dim values value-input-option]
  (let [value-range (-> (ValueRange.)
                        (.setValues values)
                        (.setMajorDimension major-dim))]
    (-> (sheets)
        (.spreadsheets)
        (.values)
        (.update sheet-id (str sheet-name "!" range) value-range)
        (.setValueInputOption value-input-option)
        (.execute))))

(def RAW "RAW")
(def USER-ENTERED "USER_ENTERED")

(defn write-columns
  ([sheet-id sheet-name range values]
   (write-columns sheet-id sheet-name range values RAW))
  ([sheet-id sheet-name range values value-input-option]
   (write-cells-values sheet-id sheet-name range "COLUMNS" values value-input-option)))

(defn write-rows
  ([sheet-id sheet-name range values]
   (write-rows sheet-id sheet-name range values RAW))
  ([sheet-id sheet-name range values value-input-option]
   (write-cells-values sheet-id sheet-name range "ROWS" values value-input-option)))

(defn- update-cells-values [sheet-id sheet-name range major-dim update-fn value-input-option]
  (let [values (get ((case major-dim
                       :row rows
                       :column columns) sheet-id sheet-name range)
                    "values")
        updated-values (update-fn values)]
    ((case major-dim
       :row write-rows
       :column write-columns) sheet-id sheet-name range updated-values value-input-option)))

(defn update-rows
  ([sheet-id sheet-name range update-fn] (update-rows sheet-id sheet-name range update-fn RAW))
  ([sheet-id sheet-name range update-fn value-input-option] (update-cells-values sheet-id sheet-name range :row update-fn value-input-option)))

(defn update-columns
  ([sheet-id sheet-name range update-fn] (update-columns sheet-id sheet-name range update-fn RAW))
  ([sheet-id sheet-name range update-fn value-input-option] (update-cells-values sheet-id sheet-name range :column update-fn value-input-option)))
