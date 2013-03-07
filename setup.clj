(use '[datomic.api :only [q db] :as d])

;; store database uri
(def uri "datomic:mem://querysample")

;; create database
(d/create-database uri)

;;(d/delete-database uri)

;; connect to database
(def conn (d/connect uri))


;; SET UP SCHEMA
;; parse schema dtm file
(def schema-tx (read-string (slurp "./schema.dtm")))

;; submit schema transaction
@(d/transact conn schema-tx)


;; POPULATE EXAMPLE SCHEMA WITH SEED DATA
;; parse seed data dtm file
(def data-tx (read-string (slurp "./seeds.dtm")))

;; submit seed data transaction
@(d/transact conn data-tx)


(def mydb (db conn))

(def department_id (ffirst (q '[:find ?dept
                                :in $ ?dept_name
                                :where [?dept :department/name ?dept_name]]
                              mydb "Sales")))

(def employees (q '[:find ?employee
                       :in $ ?dept_id
                       :where [?employee :employee/department ?dept_id]]
                  mydb department_id))

(vec (map #(d/touch (d/entity mydb (first %))) (vec employees)))


;; This works
(q '[:find ?fname ?lname ?email
     :in $ ?dept_id
     :where [?employee :employee/department ?dept_id]
     [?employee :employee/firstname ?fname]
     [?employee :employee/lastname ?lname]
     [?employee :employee/email ?email]]
   mydb department_id)


;; This throws an error
(q '[:find ?fname ?lname ?email ?id
     :in $ ?dept_id
     :where [?employee :employee/department ?dept_id]
     [?employee :employee/firstname ?fname]
     [?employee :employee/lastname ?lname]
     [?employee :employee/email ?email]
     [?employee :db/id ?id]]
   mydb department_id)


;; The embarassing answer comes from looking at the statement above
;; that binds the variable employees. Just include ?employee in the
;; list of attributes being returned. That will place the value of
;; :db/id in the collection returned from the query. Duh.
(q '[:find ?employee ?fname ?lname ?email
     :in $ ?dept_id
     :where [?employee :employee/department ?dept_id]
     [?employee :employee/firstname ?fname]
     [?employee :employee/lastname ?lname]
     [?employee :employee/email ?email]]
   mydb department_id)
