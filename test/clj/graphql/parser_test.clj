(ns graphql.parser-test
  (:require [graphql.parser :as sut]
            [clojure.test :refer :all]))

(def tests-data
  [#_["{ user(id: 2) { id, name} }",{}]
   ["type User {
    id: Integer!
    name: String
    birthDate: Date
  }", {:__type "User"
       :fields {:id "Integer!"
                :name "String"
                :birthDate "Date"}}]])

(deftest test-this-parser
  (doseq [[s e] tests-data]
    (is (= (sut/parse s) e))))

