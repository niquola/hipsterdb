(ns oauth.util
  (:import
    org.apache.commons.codec.binary.Base64
    org.apache.commons.codec.net.URLCodec))

(defmacro chain [& args]
  `(-> ~@(reverse args)))

(defn not-blank? [s]
  (or (and (string? s)
           (not (clojure.string/blank? s)))))

(defn to-utf  [s]
  (.getBytes s "UTF-8"))

(def url-codec (URLCodec.))

(defn url-encode [s]
  (.. url-codec (encode (str s)) (replaceAll "\\+" "%20")))

(defn url-decode [s]
  (. url-codec (decode s)))

(defn base64-encode [x]
  (String.  (Base64/encodeBase64  (to-utf x))))

(defn base64-decode [x]
  (String.  (Base64/decodeBase64  (to-utf x))))

(defn bearer-token [key secret]
  (base64-encode (str (url-encode key) ":" (url-encode secret))))

(defn parse-bearer-token [token]
  (map url-decode
       (-> (base64-decode token)
           (clojure.string/split #":"))))

(defn parse-authorization-header [header]
  (->
    (clojure.string/replace header #"^Basic " "")
    (parse-bearer-token)))

(defn mk-authorization-header [id secret]
  (str "Basic " (bearer-token id secret)))


(defn encode-params  [m]
  (clojure.string/join
    "&"
    (for  [[k v] m]
      (when  v
        (str  (url-encode (name k)) "="  (url-encode v))))))

(defn add-params-to-uri [uri params]
  (let [uri (java.net.URI. uri)
        query (.getQuery uri)
        new-query (str (when query (str query "&")) (encode-params params))
        new-uri (java.net.URI.
                  (.getScheme uri)
                  (.getUserInfo uri)
                  (.getHost uri)
                  (.getPort uri)
                  (.getPath uri)
                  new-query
                  (.getFragment uri))]
    (.toString new-uri)))

(defn url
  "Grabbed from hiccup.util.
  Creates a URI instance from a variable list of arguments and an optional
  parameter map as the last argument. For example:
  (url \"/group/\" 4 \"/products\" {:page 9})
  => \"/group/4/products?page=9\""
  [& args]
  (let  [params  (last args)
         args  (butlast args)
         uri-str (apply str args)
         uri (java.net.URI. uri-str)]
    (str uri-str
         (if  (map? params)
           (str (if (.getQuery uri) "&" "?")
                (encode-params params))
           params))))

(defn access-token-responce [session params]
  {:body (merge params
                {:token_type "bearer"}
                (select-keys session [:access_token :expires_at :refresh_token]))
   :headers {"Content-Type" "application/json;charset=UTF-8"
             "Cache-Control" "no-store"
             "Pragma" "no-cache"}
   :status 200})


