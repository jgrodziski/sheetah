(ns sheetah.tree)

(def EMPTY_CELL "")
(defn empty-cell? [s]
  (= EMPTY_CELL s))

(defn- empty-cells-count
  "given a vector of value count the number of empty (or blank) value following a not empty one"
  ([v]
   (empty-cells-count v (count v)))
  ([v length]
   (empty-cells-count v length empty-cell?))
  ([v length blank?]
   (let [vlength (count v)
         missing-values? (fn [length vlength idx]
                           (and (= idx (- vlength 1));last item in v
                                (not= vlength length);the usual vector length is different from this one (can occur with gsheet)
                                ))
         empty-cells-to-fill (- length vlength 1)]
     (loop [v v
            idx 0
            prev []]
       (let [curr (first v)]
         (if curr
           (if (blank? curr)
             ;;increase prev-count
             (if (empty? prev)
               (conj prev 0)
               (if (missing-values? length vlength idx)
                 (recur (rest v) (inc idx) (conj prev empty-cells-to-fill))
                 (recur (rest v) (inc idx) (update-in prev [(- (count prev) 1)] inc))))
             ;;otherwise start with 0
             (if (missing-values? length vlength idx)
               (recur (rest v) (inc idx) (conj prev empty-cells-to-fill))
               (recur (rest v) (inc idx) (conj prev 0))))
           prev))))))


(defn- col-range [idx empty-cells-count]
  (let [start (inc idx)
        end (+ start empty-cells-count)]
    [start end]))

(defn- slice-by-rows [columns [start end :as rows-range]]
  (vec (map (fn [coll]
              (if (<= start end (count coll))
                (subvec (vec coll) start (min end (count coll)))
                (subvec (vec coll) start (count coll)))) columns)))

(defn- fill-empty-cells
  "filled the inner columns until all are the size of the max one and fill with EMPTY_CELL"
  ([columns]
   (fill-empty-cells columns (apply max (map count columns))))
  ([columns max-length]
   (vec (map (fn [subcolumns] (concat subcolumns (repeat (- max-length (count subcolumns)) EMPTY_CELL))) columns))))

(defn tree [columns]
  (when (not (empty? columns))
    (let [max-length (apply max (map count columns))
          [firstcol :as filled-columns] (fill-empty-cells columns max-length)
          x->empty-cells-count (zipmap (filter (comp not empty-cell? ) firstcol)
                                       (empty-cells-count (vec firstcol) max-length))]
      (loop [xs firstcol
             idx 0
             acc []]
        (let [x (first xs)]
          (if x
            (if (and x (not (empty-cell? x)) (> (x->empty-cells-count x) 0))
              (let [slice (slice-by-rows (rest filled-columns) (col-range idx (x->empty-cells-count x)))]
                (recur (rest xs) (inc idx) (if slice (conj acc x (tree slice)) (conj acc x))))
              (recur (rest xs) (inc idx) (if (empty-cell? x) acc (conj acc x))))
            acc))))))

(defn tree-with-idx
  "Given a \"tree\" structure in the google sheet like:
  |A    |B    |C    |D    |
  |lvl1 |     |     |     |
  |     |lvl2 |     |     |
  |     |     |lvl3a|     |
  |     |     |lvl3b|     |
  |     |     |     |lvl4a|
  the Google sheets returns a vector of columns but with various size depending on empty cells following filled cells
  [[\"lvl1\"] [\"\" \"lvl2] [\"\" \"\" \"lvl3a\" \"lvl3b\"] [\"\" \"\" \"\" \"\" \"lvl4a\"]]
  tree-with-idx returns a vector tree structure including the row index of the value and the value
  => [[{:row 0, :attr \"lvl1\"} [[{:row 1, :attr \"lvl2\"} [{:row 2, :attr \"lvl3a\"} [{:row 3, :attr \"lvl3b\"} [{:row 4, :attr \"lvl4a\"}]]]]]]]
  "
  ([columns]
   (tree-with-idx 0 columns))
  ([offset columns]
   (when (not (empty? columns))
     (let [max-length (apply max (map count columns))
           [firstcol :as filled-columns] (fill-empty-cells columns max-length)
           x->empty-cells-count (zipmap (filter (comp not empty-cell? ) firstcol)
                                        (empty-cells-count (vec firstcol) max-length))]
       (loop [xs firstcol
              idx 0
              acc []]
         (let [x (first xs)
               rootidx (+ offset idx)
               node {:val x :row rootidx}]
           (if x
             (if (and x (not (empty-cell? x)) (> (x->empty-cells-count x) 0))
               (let [slice (slice-by-rows (rest filled-columns) (col-range idx (x->empty-cells-count x)))]
                 (recur (rest xs) (inc idx) (if slice (conj acc [node (tree-with-idx (inc rootidx) slice)]) (conj acc node))))
               (recur (rest xs) (inc idx) (if (empty-cell? x) acc (conj acc node))))
             acc)))))))

(comment (tree-with-idx [[:a ""  ""  :b :c :d ""  ""  ""  ""  ""  ""   ""    ""]
                         ["" :aa :ab "" "" "" :da :db :dc :de :df ""   ""    ""]
                         ["" ""  ""  "" "" "" ""  ""  ""  ""  ""  :dfa ""    ""]
                         ["" ""  ""  "" "" "" ""  ""  ""  ""  ""  ""   :dfaa ""]])
         (tree-with-idx [["lvl1"] ["" "lvl2"] ["" "" "lvl3a" "lvl3b"] ["" "" "" "" "lvl4a"]])
         ;; => [[{:row 0, :attr "lvl1"} [[{:row 1, :attr "lvl2"} [{:row 2, :attr "lvl3a"} [{:row 3, :attr "lvl3b"} [{:row 4, :attr "lvl4a"}]]]]]]]
         )


(defn treemap-with-idx
  "From a tree vector returned by the tree-with-idx function returns a tree map with value as {val [row-index {val [row-index ...]}]}"
  ([tree]
   (treemap-with-idx tree 0))
  ([tree depth]
   (when (not (empty? tree))
     (let [branch? vector?
           children rest]
       (loop [c tree
              acc {}]
         (if (branch? (first c))
           (let [{val :val row :row :as node} (ffirst c)
                 ]
             (recur (children c) (assoc acc val [row (treemap-with-idx (-> c first second) (inc depth))])))
           (let [{val :val row :row :as node} (first c)
                 ]
             (if (not (second c))
               (if val (assoc acc val [row nil]) acc)
               (recur (children c) (assoc acc val [row nil]))))))))))

(defn assoc-table-rows
  "Given an indexed treemap and the normalized rows data of the corresponding structure,
  associate the data of each row in the treemap"
  [treemap rows]
  (into {} (map (fn [[field [row child]]]
                  (let [child (if child (assoc-table-rows child rows) nil)
                        row-datas (if (>= row (count rows))
                                    nil
                                    (nth rows row))]
                    [field [row-datas child]])) treemap)))

(defn filter-treemap [treemap filter-fn]
  (if treemap
    (reduce-kv (fn [acc field [infos child :as val]]
                 (if (filter-fn field val )
                   (assoc acc field [infos (filter-treemap child filter-fn)])
                   acc)) {} treemap)
    nil))


(comment
  (tree [[:a ""  ""  :b :c :d ""  ""  ""  ""  ""  ""   ""    ""]
         ["" :aa :ab "" "" "" :da :db :dc :de :df ""   ""    ""]
         ["" ""  ""  "" "" "" ""  ""  ""  ""  ""  :dfa ""    ""]
         ["" ""  ""  "" "" "" ""  ""  ""  ""  ""  ""   :dfaa ""]])
  ;;=>
  (def result [[:a [:aa :ab]] :b :c [:d [:da :db :dc :de [:df [:dfa [:dfaa]]]]]])
  (treemap-with-idx (tree-with-idx [["lvl1"] ["" "lvl2"] ["" "" "lvl3a" "lvl3b"] ["" "" "" "" "lvl4a"]]))
  ;; {"lvl1" [0 {"lvl2" [1 {"lvl3a" [2 nil], "lvl3b" [3 {"lvl4a" [4 nil]}]}]}]}

  (-> (sheet/columns "16Q1iN4mJ_-nURLQxTdoWoYoal7YxuvcdKo3tgjCjm1M" "Diffusion-API-Endpoints" "noticesInitResponseStructure")
      (get "values"))
  )
