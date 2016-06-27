(ns oauth.core
  (:require [form :as f]
            [formats :as fmt]
            [db :as db]
            [oauth.util :as util]
            [oauth.jwt :as jw]))

(defn todo [req]
  {:body "TODO"})

(defn layout [cnt]
  {:headers {"Content-Type" "hiccup"} 
   :body
   [:html
    [:head
     [:link {:rel "stylesheet"
             :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css"
             :integrity "sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7"
             :crossorigin "anonymous"}]]
    [:body cnt]]})


(defn -form [data]
  [:div.container
   [:div.row
    [:div.col-md-6.col-md-offset-3
     (f/form-for
      input
      {:action "" :method "POST" :data (dissoc data :password) :title "Sign In"}
      (when-let [msg (:debug data)] [:pre msg])
      (when-let [err (:error data)] [:div.alert.alert-danger err])
      (input :user :text {:required true})
      (input :password :password {:required true})
      (f/form-actions
       [:button.btn.btn-success "Sign In"]))]]])

(defn $form
  {:swagger {:summary "Sign In Form"}}
  [{data :params :as req}]

  (layout (-form data)))

"
set role user
select * from pg_authid where rolpassword = 'md5' || md5('rootroot');
select current_user
SET [ SESSION | LOCAL ] SESSION AUTHORIZATION username
"

(defn *sign-in [{usr :user pswd :password}]
  (db/with-db "postgres"
    (db/query-first
     {:select [:*]
      :from [:pg_authid]
      :where [:= :rolpassword
              (db/raw (str "'md5' || md5('" pswd usr"')"))]})))

(defn -creds [token]
  [:div.container
   [:br]
   [:br]
   [:h3 "JWT token"]
   [:div token]
   [:pre (str (jw/decode token))]])

(defn mk-claim [session]
  (-> {:iss "hipsterdb"
       :exp (time/plus (time/now) (time/days 1))
       :iat (time/now)
       :sub (:rolname session)}
      jw/sign))

(defn oauth-request? [params]
  (:redirect_uri params))

(defn get-token-from-request [{{token :access_token} :params
                               {auth "authorization"} :headers
                               :as req}]
  (or token
      (and auth
           (re-matches #"^Bearer .*" auth)
           (clojure.string/replace auth #"^Bearer " ""))))

(defn wrap-token [h]
  (fn [req]
    (if-let [token (get-token-from-request req)]
      (do
        (println token (jw/decode token))
        (h (assoc req :jwt (jw/decode token))))
      (h req))))

(defn redirect-with-token [params session]
  (let [uri (util/add-params-to-uri (:redirect_uri params) session)]
    #_(layout [:h3 uri])
    {:status  303
     :headers  {"Location" uri}}))

(defn $form!
  {:swagger {:summary "Sign In Form"}}
  [{data :params :as req}]
  (if-let [session (*sign-in data)]
    (let [token (mk-claim session)]
      (if (oauth-request? data)
        (redirect-with-token data {:access_token token})
        (layout (-creds token))))
    (layout (-form (assoc data :error "Wrong credentials")))))

(def routes
  {:GET #'$form
   :POST #'$form!
   "token"     {:GET #'todo
                :POST #'todo
                "deny"      {:GET #'todo}}
   "authorize" {:GET #'$form
                :POST #'todo}
   "refresh"   {:POST #'todo}
   "session"   {:DELETE #'todo}})
