(ns bones.event-source
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [schema.core :as s])
  (:require [com.stuartsierra.component :as component]
            [cljs.core.async :as a]
            ))


(defrecord EventSource [stream state url msg-ch listener-loop]
  component/Lifecycle
  (start [cmp]
    (if (and (:stream cmp) (= 1 (:state cmp)))
      (do
        (println "already started stream")
        cmp)
      (do
        (let [src (js/EventSource. (:url cmp) #js{ :withCredentials true } )]
          (set! (.-onmessage src) (fn [ev]
                                    (js/console.log "onmessage")
                                    (a/put! (:msg-ch cmp) ev.data)))
          (set! (.-onerror src) (fn [ev]
                                  (js/console.log "onerror")
                                  (js/console.log (.stringify js/JSON ev.data))))
          (set! (.-onopen src) (fn [ev]
                                 (js/console.log "EventSource listening")))
          (-> cmp
              (assoc :stream src)
              (assoc :state (.-readyState src))
              )))))
  (stop [cmp]
    (if (:stream cmp)
      (do
        (println "closing stream")
        (.close (:stream cmp))
        (dissoc cmp :stream))
      (do
        (println "stream already closed")
        cmp))))

(defn event-source [url msg-ch]
  (map->EventSource {:url url :msg-ch msg-ch}))

(comment
  (def msg-ch (a/chan))

  (def es (event-source
           "http://localhost:3000/api/events?topic=userspace.jobs..wat-output"
           msg-ch))
  (component/start es)
  (:stream es)
  (component/stop es)
  )