(ns sheetah.2darray
  (:require [clojure.string :refer [trim]]))


(defn normalize-details
  "Normalize the details by giving arbitrary names to the columns, adding predicate on the
  syntax of column's cells et having the possibility to give  a default value
  `details` is a list of the rows's details
  `cols` is a vector containing map containg two entry with keys :name for the column name and :fn that process the value or return the found value if missing. The order needs to be the same as the columns in the sheet"
  [details cols]
  (let [normalize-fn (fn [row]
                       (let [norm-length (count cols)
                             row-length (count row)
                             norm-length-row (if (> row-length norm-length)
                                               (take norm-length row)
                                               (concat row (repeat (- norm-length row-length) "")))]
                         (loop [idx 0
                                vals norm-length-row
                                res {}]
                           (if (first vals)
                             (let [{:keys [name fn]} (nth cols idx)
                                   val (first vals)
                                   norm-val (if fn
                                              (fn val)
                                              val)]
                               (recur (inc idx)
                                      (rest vals)
                                      (assoc res name norm-val)))
                             res))))]
    (map #(->> % (map trim) normalize-fn) details)))

;; ;;================================================
;; (def details [["1ere desc" "true" "inttttteger"]
;;               ["2e desc" "false"]
;;               ["" "" "object"]])

;; (def cols [{:name :descr}
;;            {:name :req :fn (fn [x] (or (= x "true") false))}
;;            {:name :type :fn (fn [x] (if (accepted-type? x) x "string"))}])

;; (normalize-details-v2 details cols)
;; ;;================================================


(defn assoc-fields-details
  "Given an indexed treemap and the normalized details of the corresponding structure,
  associate the details of each row in the treemap"
  [treemap details]
  (into {} (map (fn [[field [row child]]]
                  (let [child (if child (assoc-fields-details child details) nil)
                        row-details (if (>= row (count (:rows details)))
                                      nil
                                      (nth details row))]
                    [field [row-details child]])) treemap)))


