(ns oauth.jwt
  (:require [clj-jwt.core  :as jwt]
            [clj-jwt.key   :as jwt-key]
            [clj-time.core :as time]))

(def rsa-prv-key (jwt-key/private-key "private.pem"))
(def rsa-pub-key (jwt-key/public-key "public.pem"))

(defn sign [claim]
  (-> claim
      jwt/jwt
      (jwt/sign :RS256 rsa-prv-key)
      jwt/to-str))


(defn verify [token]
  (-> token
      jwt/str->jwt
      (jwt/verify rsa-pub-key)))

(defn read [token]
  (when (verify token)
    (:claims (jwt/str->jwt token))))

(comment 

  (def claim
    {:iss "hipsterdb"
     :exp (time/plus (time/now) (time/days 1))
     :sub "user"
     :iat (time/now)})

  (def token (-> claim sign))

  (read token)

  )
