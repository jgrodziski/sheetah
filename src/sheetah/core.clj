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

(defn sheets []
  (let [transport (GoogleNetHttpTransport/newTrustedTransport)
        sheets (-> (Sheets$Builder. transport JSON_FACTORY (credentials transport))
                   (.setApplicationName APPLICATION_NAME)
                   (.build))]
    sheets))

(defn cells-values [sheet-id sheet-name range major-dim]
  (-> (sheets)
      (.spreadsheets)
      (.values)
      (.get sheet-id (str sheet-name "!" range))
      (.setMajorDimension major-dim)
      (.execute)))

(defn columns [sheet-id sheet-name range] (cells-values sheet-id sheet-name range "COLUMNS"))
(defn rows    [sheet-id sheet-name range] (cells-values sheet-id sheet-name range "ROWS"))

(defn write-cells-values [sheet-id sheet-name range major-dim values value-input-option]
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