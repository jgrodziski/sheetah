(ns sheetah.table
  (:require [clojure.string :refer [trim]]))

(defn normalize
  "Normalize the data found in the tabular structure by giving arbitrary names to the columns, adding predicate on the syntax of column's cells
  `table` is a list of the rows's datas as a vector of vector (aka. a table)
  `cols` is a vector of maps containing two entry with keys :name for the column name and :fn that process the value or return the found value if missing. The order needs to be the same as the columns in the sheet
  if the "
  [table cols]
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
    (map #(->> % (map trim) normalize-fn) table)))



