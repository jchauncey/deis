(ns deis.nginx
  (:require [clojure.string :as str]
            [riemann.config :as config]
            [riemann.streams :as streams]
            [riemann.common :as common]))

(defn get-response-code [log]
  (get log 4))

(defn valid-request? [parsed-nginx-log]
  (-> parsed-nginx-log 
      (count)
      (> 2)))

(defn split-nginx-log [nginx-log]
  (try
    (str/split nginx-log #" - ")
    (catch Exception e
      nil)))

(defn parse-nginx-log [event]
  (let [nginx-log (:description event)
        parsed-nginx-log (split-nginx-log nginx-log)]
    (if (valid-request? parsed-nginx-log)
      parsed-nginx-log
      nil)))

(defn get-response-time [log]
  (let [response-time (get log 12)]
    (if (not (= "-" response-time))
      (.intValue (* 1000 (Float. response-time)))
      0)))

(defn get-service-host [log]
  (let [host-and-port (str/split (get log 10) #":")]
    (get host-and-port 0)))

(defn get-service-name [log]
  (-> (get log 11)
      (str/split #"\.")
      (get 0)))
  
(defn service-response-time-event [log]
  (let [response-time (get-response-time log)]
    (if (> response-time 0)
      (config/reinject (common/event {:service (str (get-service-name log) "-response-time")
                                      :host (get-service-host log)
                                      :metric response-time
                                      :state "ok"})))))

(defn service-response-code-event [log]
  (config/reinject (common/event {:service (str (get-service-name log) "-response-code-" (get-response-code log))
                                  :host  (get-service-host log)
                                  :metric 1
                                  :state "ok"})))

(defn service-request-event [log]
  (config/reinject (common/event {:service (str (get-service-name log) "-request")
                                  :host (get-service-host log)
                                  :metric 1
                                  :state "ok"
                                  :ttl 60 })))

(defn router-response-time-event [host log]
  (let [response-time (get-response-time log)]
    (if (> response-time 0)
      (config/reinject (common/event {:service "deis-router-response-time"
                                      :host host
                                      :metric response-time
                                      :state "ok"})))))

(defn router-response-code-event [host log]
  (config/reinject (common/event {:service (str "deis-router-response-code-" (get-response-code log))
                                  :host  host
                                  :metric 1
                                  :state "ok"})))

(defn router-request-event [host]
  (config/reinject (common/event {:service "deis-router-request"
                           :host  host
                           :metric 1
                           :state "ok"
                           :ttl 60})))

(defn request-rate [event]
  (println (str (:service event) "-rate"))
  (streams/with {:ttl 5
                 :metric 1 
                 :state "ok"
                 :service (str (:service event) "-rate")} 
    (streams/rate 1 config/reinject)))

(defn create-nginx-metrics [event]
  (when-let [log (parse-nginx-log event)]
    (service-request-event log)
    (service-response-code-event log)
    (service-response-time-event log)
    (router-request-event (:host event))
    (router-response-time-event (:host event) log)
    (router-response-code-event (:host event) log)))



