# overtone.device.launchpad

Interact with the Novation Launchpad from within Clojure.

## Usage

    (def grid (make-launchpad))
    
    ;; Interact with the main body of buttons using the Grid protocol
    ;; from overtone.device.grid:
    
    (led-set grid 1 2 1) ; red
    (led-set grid 1 2 2) ; green
    (led-set grid 1 2 3) ; yellow
    (led-set grid 1 2 0) ; off
    (on-action grid :handler-name (fn [event x y] (led-set grid x y
        (if (= :press event) 1 0))))

    ;; Chessboard pattern
    (led-frame lp
       (into {}
         (for [x (range 8)
               y (range 8)]
           [[x y] (if (zero? (rem (+ x y) 2)) 1 2) ] )))
    
    ;; Interact with the peripheral circular buttons using the
    ;; MetaKeys protocol:
    (meta-on-action grid (fn [event key] (meta-led-set grid key (if (= :press event) 2 0))))

## Contributors

* Philip Potter
* Gary Trakhman
* Fronx
