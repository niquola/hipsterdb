(ns form
  (:require [clojure.string :as cs]
            [ring.middleware.anti-forgery :as rma]))

(defn uuid  []  (str  (java.util.UUID/randomUUID)))

(defn- default-i18n  [k]
  (->> (cs/split (name k) #"_")
       (map cs/capitalize)
       (cs/join " ")))

(defn mk-input [{scope :scope i18n :i18n data :data errors :errors}]
  (fn [name-kw type-kw & [{label :label :as i-opts}]]
    (let [i-id     (uuid)
          i-name   (if scope
                     (str (name scope) "[" (name name-kw) "]")
                     (name name-kw))
          i-type   (name type-kw)
          i18n     (or i18n default-i18n)
          label    (cond
                     (= label false) nil
                     (not (clojure.string/blank? label)) label
                     :else (i18n name-kw))
          i-value  (get data name-kw)
          errs     (get errors name-kw)
          i-opts   (merge (or i-opts {})
                          {:id i-id :name i-name :value i-value :type i-type})]
      [:div.form-group {:class (when errs "has-error")}
       (when label
         [:label.control-label {:for i-id} label (when (:required i-opts) "*")])
       (cond
         (= type-kw :textarea) [:textarea.form-control i-opts i-value]
         :else [:input.form-control i-opts])
       (when errs [:span.help-block (cs/join ", " errs)])])))

(defn anti-forgery-input []
  [:input {:type "hidden"  :name "__anti-forgery-token" :value rma/*anti-forgery-token*}])

(defmacro form-for [inp opts & cnt]
  `(let [input# (mk-input ~opts)]
     (let [~(symbol inp) input#]
       [:form ~opts
        (anti-forgery-input)
        ~@cnt])))

(defn form-actions [& actions]
  (into
    [:div.form-actions]
    actions))

(defn form-errors
  ([errors]
   (form-errors errors {}))
  ([errors {i18n :i18n}]
   (when errors
     [:div.alert.alert-danger
      (if (string? errors)
        errors
        [:ul.list-unstyled
         (for [[k errs] errors]
           [:li [:b (cond
                      (vector? k) (cs/join " " (map (or i18n default-i18n) k))
                      :else ((or i18n default-i18n) k))]
            "&nbsp;" (cs/join ", " errs)])])])))

(comment
  (def strings {:name "Name"})

  (defn i18n [k]
    (or (get strings k) (name k)))

  (defn -view [{{data :user} :params errors :errors :as req}]
    (v/-layout
      req
      [:div.container
       (form-for
         input {:data data :errors errors :method "POST" :action "" :i18n i18n :scope "user"}
         [:form
          [:h3 "New entity"]
          (form-errors errors {:i18n i18n})
          (input :name :text {:required true})
          (input :email :email)
          (input :desc :textarea)
          (form-actions
            [:button.btn.btn-success "Save"]
            [:a {:href "#"} "Cancel"])])]))

  (defn $form [req]
    (u/ok (-view req)))

  (defn $post [req]
    (u/ok (-view (merge req {:errors {:name ["wrong name"]}})))))
