# quantum.numeric.statistics

## Big data

https://datasketches.github.io/

In the analysis of big data there are often problem queries that don’t scale because they require huge compute resources and time to generate exact results. Examples include count distinct, quantiles, most frequent items, joins, matrix computations, and graph analysis.
If approximate results are acceptable, there is a class of specialized algorithms, called (stochastic) streaming algorithms / sketches that can produce results orders-of magnitude faster and with mathematically proven error bounds. For interactive queries there may not be other viable alternatives, and in the case of real-time analysis, sketches are the only known solution.

This technology has helped Yahoo successfully reduce data processing times from days to hours or minutes on a number of its internal platforms.
