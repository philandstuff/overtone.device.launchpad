(ns overtone.device.test.launchpad
  (:use [overtone.device.launchpad] :reload)
  (:use [midje.sweet])
  (:import (javax.sound.midi ShortMessage)))

(fact "MIDI note 0 corresponds to coordinate [0 0], top left"
  (midi-note->coords 0) => [0 0])

(fact "MIDI note 119 corresponds to coordinate [7 7], bottom right"
  (midi-note->coords 119) => [7 7])

(facts "The x coordinate is given by the midi note mod 16"
  (fact (first (midi-note->coords 1)) => 1)
  (fact (first (midi-note->coords 2)) => 2)
  (fact (first (midi-note->coords 7)) => 7)
  (fact (first (midi-note->coords 16)) => 0)
  (fact (first (midi-note->coords 23)) => 7)
  (fact (first (midi-note->coords 35)) => 3))

(facts "The y coordinate is given by the midi note divided by 16 (ignoring remainder)"
  (fact (second (midi-note->coords 0)) => 0)
  (fact (second (midi-note->coords 7)) => 0)
  (fact (second (midi-note->coords 16)) => 1)
  (fact (second (midi-note->coords 23)) => 1)
  (fact (second (midi-note->coords 32)) => 2)
  (fact (second (midi-note->coords 112)) => 7))

(fact "Coord [0 0] corresponds to midi note 0"
  (coords->midi-note 0 0) => 0)

(fact "Coord [7 0] corresponds to midi note 7"
  (coords->midi-note 7 0) => 7)

(facts "Coord [x y] corresponds to midi note (+ (* 16 y) x)"
  (fact (coords->midi-note 1 4) => 65)
  (fact (coords->midi-note 5 2) => 37))

(defn two-byte-message [expected]
  {:pre [(isa? (class expected) ShortMessage)]}
   (chatty-checker [actual]
       (and (isa? (class actual) ShortMessage)
            (= (.getCommand actual) (.getCommand expected))
            (= (.getChannel actual) (.getChannel expected))
            (= (.getData1 actual) (.getData1 expected))
            (= (.getData2 actual) (.getData2 expected)))))

(fact "make-ShortMessage can create valid note-on messages, and defaults to channel 0"
  (make-ShortMessage :note-on 4 6) => (two-byte-message (doto (ShortMessage.) (.setMessage 0x90 0 4 6))))

(fact "make-ShortMessage can create valid note-on messages on different channels"
  (make-ShortMessage 2 :note-on 4 6) => (two-byte-message (doto (ShortMessage.) (.setMessage 0x90 2 4 6))))

(fact "Metakeys can be looked up via a Midi event"
  (get-metakey {:cmd ShortMessage/NOTE_ON :note 24 :vel 127}) => :pan
  (get-metakey {:cmd ShortMessage/CONTROL_CHANGE :note 111}) => :mixer)

(facts "key-coords"
  (fact "Events from the square buttons are key events"
    (key-coords {:cmd ShortMessage/NOTE_ON :vel 127 :note 0   }) => [0 0]
    (key-coords {:cmd ShortMessage/NOTE_ON :vel 0   :note 0x30}) => [0 3]
    (key-coords {:cmd ShortMessage/NOTE_ON :vel 127 :note 0x77}) => [7 7])
  (fact "Events from the metakeys are not key events"
    (key-coords {:cmd ShortMessage/NOTE_ON :vel 127 :note 0x08}) => falsey
    (key-coords {:cmd ShortMessage/CONTROL_CHANGE :vel 0 :note 111}) => falsey))

(facts "event-map"
  (fact "Metakey events result in metakey maps"
    (event-map {:cmd ShortMessage/NOTE_ON :vel 127 :note 0x08}) => {:key :vol :event :press}
    (event-map {:cmd ShortMessage/CONTROL_CHANGE :vel 0 :note 111}) => {:key :mixer :event :release})
  (fact "Key events result in key maps"
    (event-map {:cmd ShortMessage/NOTE_ON :vel 127 :note 0   }) => {:key [0 0] :event :press}
    (event-map {:cmd ShortMessage/NOTE_ON :vel 0   :note 0x30}) => {:key [0 3] :event :release})
  (fact "Other events result in falsey values"
    (event-map {:cmd ShortMessage/NOTE_ON :vel 127 :note 0x09}) => falsey))

(facts "event-type"
  (fact "Metakey events are of type :launchpad-metakey"
    (event-type {:cmd ShortMessage/NOTE_ON :vel 127 :note 0x08}) => :launchpad-metakey
    (event-type {:cmd ShortMessage/CONTROL_CHANGE :vel 0 :note 111}) => :launchpad-metakey)
  (fact "Key events are of type :launchpad-key"
    (event-type {:cmd ShortMessage/NOTE_ON :vel 127 :note 0   }) => :launchpad-key
    (event-type {:cmd ShortMessage/NOTE_ON :vel 0   :note 0x30}) => :launchpad-key)
  (fact "Other events result in falsey values"
    (event-type {:cmd ShortMessage/NOTE_ON :vel 127 :note 0x09}) => falsey))

