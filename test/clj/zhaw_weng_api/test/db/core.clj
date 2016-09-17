(ns zhaw-weng-api.test.db.core
  (:require [zhaw-weng-api.db.core :refer [*db*] :as db]
            [luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [zhaw-weng-api.config :refer [env]]
            [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'zhaw-weng-api.config/env
                 #'zhaw-weng-api.db.core/*db*)
    (migrations/migrate ["migrate"] (env :database-url))
    (f)))

(deftest test-products
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (testing "generated functions from HugSQL are working"
      (let [project {:title    "Test Category 1"}
            project_id (:id (db/create-project! t-conn project))
            product {:client_id  "some-uuid"
                   :due_date   (java.util.Date.)
                   :done       false
                   :priority   "1"
                   :title      "Test Product 1"
                   :project_id project_id}
            id (:id (db/create-product! t-conn product))]

        (is (= (assoc product :id id )
               (db/get-product t-conn {:id id :project_id project_id})))

        (is (= 1
               (db/update-product!
                t-conn
                (assoc product
                       :id id
                       :title "Test Product Updated"))))

        (is (= (assoc product :id id :title "Test Product Updated")
               (db/get-product t-conn {:id id :project_id project_id})))

        (is (= 1 (db/delete-product!
                  t-conn
                  {:id id :project_id project_id})))

        (is (= nil
               (db/get-product t-conn {:id id :project_id project_id})))))))
