(ns sheetah.core
  (:require [clojure.java.io :as io])
  (:import com.google.api.client.auth.oauth2.Credential
           com.google.api.client.googleapis.auth.oauth2.GoogleCredential
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

#_(defn credentials
  ([transport]
   (credentials transport nil))
  ([transport creds]
   (with-open [rdr (if creds
                     (io/reader (io/resource creds))
                     (io/reader (io/resource "credentials.json")))]
     (let [client-secrets (GoogleClientSecrets/load JSON_FACTORY rdr)
           flow (-> (GoogleAuthorizationCodeFlow$Builder. transport JSON_FACTORY client-secrets (java.util.ArrayList. [SheetsScopes/SPREADSHEETS]))
                    (.setDataStoreFactory FILE_DATA_STORE_FACTORY)
                    (.setAccessType "offline")
                    (.build))
           receiver (-> (LocalServerReceiver$Builder.) (.setPort 8888) (.build))]
       (-> (AuthorizationCodeInstalledApp. flow receiver) (.authorize "user"))))))

(defn credentials [creds]
  (with-open [is (io/input-stream creds)]
    (->
     (GoogleCredential/fromStream is)
     (.createScoped (java.util.ArrayList. [SheetsScopes/SPREADSHEETS]) ))))


(defn sheets
  (^com.google.api.services.sheets.v4.Sheets []
   (sheets "credentials.json"))
  (^com.google.api.services.sheets.v4.Sheets [creds]
   (let [transport (GoogleNetHttpTransport/newTrustedTransport)
         sheets    (-> (Sheets$Builder. transport JSON_FACTORY (credentials creds))
                    (.setApplicationName APPLICATION_NAME)
                    (.build))]
     sheets)))

(defn alpha-uppercase? [s]
  (re-matches #"[A-Z]+" s))

(defn assert-row [x]
  (if (instance? java.lang.String x)
    (try (java.lang.Long/valueOf x) (catch NumberFormatException nfe (throw (ex-info "A row reference of the sheet in a string must be convert correctly to a long or integer" {:row x}))))
    (when (not (or (instance? java.lang.Integer x) (instance? java.lang.Long x)))
      (throw (ex-info "A row reference of the sheet must be an Integer or a long" {:row x})))))

(defn cell-value
  "return the value in a unique cell denoted by its column and row reference, e.g.: \"A\" and \"1\" for the A1 cell in the sheet"
  ([sheet-id sheet-name column row]
   (cell-value nil sheet-id sheet-name column row))
  ([creds sheet-id sheet-name column row]
   (when (not (alpha-uppercase? column)) (throw (ex-info "Column must be a string alpha with only uppercase character" {:column column})))
   (assert-row row)
   (-> (sheets creds)
       (.spreadsheets)
       (.values)
       (.get sheet-id (str sheet-name "!" column row))
       (.execute)
       (.get "values")
       (ffirst))))

(defn named-ranges
  "Return all the named ranges of the sheet, return a map with key a string of the name of the range and value of the range like {:endColumnIndex 15, :endRowIndex 237, :startColumnIndex 6, :startRowIndex 8} "
  ([sheet-id]
   (named-ranges nil sheet-id))
  ([creds sheet-id]
   (let [raw-named-ranges (-> (sheets creds)
                              (.spreadsheets)
                              (.get sheet-id)
                              (.execute)
                              (.get "namedRanges"))]
     (into {} (map (fn [{:strs [name range] :as namedRange}] [name (into {} (map (fn [[k v]] [(keyword k) v])) range)]) raw-named-ranges)))))

(defn cells-values
  ([sheet-id sheet-name range major-dim]
   (cells-values nil sheet-id sheet-name range major-dim))
  ([creds sheet-id sheet-name range major-dim]
   (-> (sheets creds)
       (.spreadsheets)
       (.values)
       (.get sheet-id (str sheet-name "!" range))
       (.setMajorDimension major-dim)
       (.execute))))

(defn columns
  ([sheet-id sheet-name range]
   (columns nil sheet-id sheet-name range))
  ([creds sheet-id sheet-name range]
   (cells-values creds sheet-id sheet-name range "COLUMNS")))

(defn rows
  ([sheet-id sheet-name range]
   (rows nil sheet-id sheet-name range))
  ([creds sheet-id sheet-name range]
   (cells-values creds sheet-id sheet-name range "ROWS")))

(defn write-cells-values
  ([sheet-id sheet-name range major-dim values value-input-option]
   (write-cells-values nil sheet-id sheet-name range major-dim values value-input-option))
  ([creds sheet-id sheet-name range major-dim values value-input-option]
   (let [value-range (-> (ValueRange.)
                         (.setValues values)
                         (.setMajorDimension major-dim))]
     (-> (sheets creds)
         (.spreadsheets)
         (.values)
         (.update sheet-id (str sheet-name "!" range) value-range)
         (.setValueInputOption value-input-option)
         (.execute)))))

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

(defn write-columns-with-creds
  ([creds sheet-id sheet-name range values]
   (write-columns-with-creds creds sheet-id sheet-name range values RAW))
  ([creds sheet-id sheet-name range values value-input-option]
   (write-cells-values creds sheet-id sheet-name range "COLUMNS" values value-input-option)))

(defn write-rows-with-creds
  ([creds sheet-id sheet-name range values]
   (write-rows-with-creds creds sheet-id sheet-name range values RAW))
  ([creds sheet-id sheet-name range values value-input-option]
   (write-cells-values creds sheet-id sheet-name range "ROWS" values value-input-option)))

(defn- update-cells-values [creds sheet-id sheet-name range major-dim update-fn value-input-option]
  (let [values (get ((case major-dim
                       :row rows
                       :column columns) creds sheet-id sheet-name range)
                    "values")
        updated-values (update-fn values)]
    ((case major-dim
       :row write-rows-with-creds
       :column write-columns-with-creds) creds sheet-id sheet-name range updated-values value-input-option)))

(defn update-rows
  ([sheet-id sheet-name range update-fn] (update-rows sheet-id sheet-name range update-fn RAW))
  ([sheet-id sheet-name range update-fn value-input-option] (update-cells-values nil sheet-id sheet-name range :row update-fn value-input-option)))

(defn update-columns
  ([sheet-id sheet-name range update-fn] (update-columns sheet-id sheet-name range update-fn RAW))
  ([sheet-id sheet-name range update-fn value-input-option] (update-cells-values nil sheet-id sheet-name range :column update-fn value-input-option)))

(defn update-rows-with-creds
  ([creds sheet-id sheet-name range update-fn] (update-rows-with-creds creds sheet-id sheet-name range update-fn RAW))
  ([creds sheet-id sheet-name range update-fn value-input-option] (update-cells-values creds sheet-id sheet-name range :row update-fn value-input-option)))

(defn update-columns-with-creds
  ([creds sheet-id sheet-name range update-fn] (update-columns-with-creds creds sheet-id sheet-name range update-fn RAW))
  ([creds sheet-id sheet-name range update-fn value-input-option] (update-cells-values creds sheet-id sheet-name range :column update-fn value-input-option)))
