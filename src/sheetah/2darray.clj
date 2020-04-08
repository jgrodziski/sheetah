(ns sheetah.2darray)

(defmulti normalize* :col)

(defn normalize-details-old
  "Normalize the given details.
  Each row should have n items where n is the length of `col-name`.
  The default value for Type is string, for Required False and for everything else `default-value`

  (normalize-detail [[string TRUE] [integer FALSE yyyyy]] [:type :required :description])
  => ({:type string :required TRUE :description -} {:type integer :required FALSE :description yyyyy})"
  ([details col-names]
   (normalize-details details col-names "none"))
  ([details col-names default-value]
   (let [normalize-fn (fn [row]
                        (let [norm-length     (count col-names)
                              row-length      (count row)
                              norm-length-row (if (> row-length norm-length)
                                                (take norm-length row)
                                                (concat row (repeat (- norm-length row-length)
                                                                    default-value)))]
                          (loop [col-names col-names
                                 vals      norm-length-row
                                 res       {}]
                            (if (first col-names)
                              (let [col (first col-names)
                                    val (first vals)
                                    normalized-val (normalize* {:col (first col-names) :val (first vals) })]
                                (recur (rest col-names) (rest vals) (assoc res col normalized-val)))
                              res))))]
     (map #(->> % (map trim) normalize-fn) details))))

(defn normalize-details
  "Normalize the details by giving arbitrary names to the columns, adding predicate on the
  syntax of column's cells et having the possibility to give  a default value

  `details` is a list of the rows's details
  `col-names` is a vector containing arbitrary names for the columns. The order needs to be the same as the columns in the sheet
  `options` is a map containing options (predicates, default values)
  The keys are the names in `col-names` and the values are maps containing the keys :predicate and :default. In :predicate you can put a boolean function that will the check is the values of each cells of the column are correct. If a value isn't correct, it will be replaced by value contained in :default.
  You can also choose a global default value with the key :global-default. If no default values are choosen for a specific column, the default value will be given by :global-default and the key isn't in `options`, \"none\" is the default value."
  [details col-names options]
  (when (some #{:global-default} col-names)
    (throw (Exception. "You can't use :global-default as a name of a column")))
  (let [default-values (mapv (fn [col]
                               (or (get-in options [col :default])
                                   (:global-default options)
                                   "none")) col-names)
        normalize-fn (fn [row]
                       (let [norm-length (count col-names)
                             row-length (count row)
                             norm-length-row (if (> row-length norm-length)
                                               (take norm-length row)
                                               (concat row (subvec default-values (count row) (count default-values))))]
                         (loop [idx 0
                                col-names col-names
                                vals norm-length-row
                                res {}]
                           (if (first col-names)
                             (let [col (first col-names)
                                   val (first vals)
                                   pred (get-in options [col :predicate])
                                   norm-val (if (or (empty? val)
                                                    (and pred
                                                         (not (pred val))))
                                              (nth default-values idx)
                                              val)]
                               (recur (inc idx)
                                      (rest col-names)
                                      (rest vals)
                                      (assoc res col norm-val)))
                             res))))]
    {:rows (map #(->> % (map trim) normalize-fn) details)
     :default-row (zipmap col-names default-values)}))


(defn assoc-fields-details
  "Given an indexed treemap and the normalized details of the corresponding structure,
  associate the details of each row in the treemap"
  [treemap details]
  (into {} (map (fn [[field [row child]]]
                  (let [child (if child (assoc-fields-details child details) nil)
                        row-details (if (>= row (count (:rows details)))
                                      (:default-row details)
                                      (nth (:rows details) row))]
                    [field [row-details child]])) treemap)))
