(ns quantum.measure.information
  (:require-quantum [ns fn logic num set err macros pr log str])
  (:require [quantum.measure.core :refer [defunits-of]]))

; Basic unit of information (entropy).  The entropy in bits
; of a random variable over a finite alphabet is defined
; to be the sum of -p(i)*log2(p(i)) over the alphabet where
; p(i) is the probability that the random variable takes
; on the value i. 

(defunits-of information [:bits #{:b}]
  :nibble [[4    :bits ] #{:nybbles    :nybles             }]
  :B      [[8    :bits ] #{:bytes      :octets             }]
  :kB     [[1024 :B    ] #{:kilobytes  :kibibytes :KiB     }]
  :MB     [[1024 :kB   ] #{:megabytes  :mebibytes :MiB     }]
  :GB     [[1024 :MB   ] #{:gigabytes  :gibibytes :GiB     }]
  :TB     [[1024 :GB   ] #{:terabytes  :tebibytes :TiB     }]
  :PB     [[1024 :TB   ] #{:petabytes  :pebibytes :PiB     }]
  :EB     [[1024 :PB   ] #{:exabytes   :exbibytes :EiB     }]
  :ZB     [[1024 :EB   ] #{:zettabytes :zebibytes :ZiB     }]
  :YB     [[1024 :ZB   ] #{:yottabytes :yobibytes :YiB     }]

  :kbits  [[1000 :bits ] #{:kilobits   :kibits    :kibibits}]
  :Mbits  [[1000 :kbits] #{:megabits   :Mibits    :mebibits}]
  :Gbits  [[1000 :Mbits] #{:gigabits   :gibibits  :gibits  }])