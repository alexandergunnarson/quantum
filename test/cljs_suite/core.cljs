; CLJS encryption for use in CLJ. Works!!
#_(go (let [res (crypto/aes "things!!" :encrypt "256Yesthismaybetemp")]
  (reset! result
    [; "FDui2saLr07tTwGFtebFmQ=="
     (-> res :encrypted .bytes conv/forge-bytes->base64)
     ; "DbsnljwknN1gJyrIjqR9VQ=="
     (-> res :key  conv/forge-bytes->base64)
     ; "uUzXwauhOdEVv9RYVTgKCWjUHiVRGgj8+pwfA4Lx5WnFQrAeQWwnFLv6t1zimsW5ih4utCl5cPokuD5Sa7rJgbNGKytuiVowkndeX1PaaY9+efsy28Ts8mYafRr0XXouDr/12f9q1l1aksHyeXSvTWthmJtCPX/6PxNLE1tzv5k="
     (-> res :salt conv/forge-bytes->base64)])))
