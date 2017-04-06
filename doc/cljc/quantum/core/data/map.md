# Map

## `java.util.HashMap`

## [`SmoothieMap`](https://github.com/OpenHFT/SmoothieMap)

`ChronicleMap` meets `HashMap`.

### General

- `-` Cannot hold less than 32 entries
- `-` Should be relatively large (> e.g 1000 entries)
- `-` Not designed for primitive values

### Computational

`put`

- `+` Worst-case latency is more than 100 times smaller than in `HashMap`
  - When inserting 10 million entries into `HashMap` the longest one (when about 6m entries are already in the map) takes about 42 milliseconds.
  - The longest insertion into SmoothieMap is only 0.27 milliseconds (when about 8m entries are already inserted).
- `~` Latency grows linearly with the map size (as with `HashMap`), though with a very small coefficient

`get`

- `~` *Amortized* performance of read (get(k)) and write operations on SmoothieMap is approximately equal to HashMap's performance (sometimes slower, sometimes faster, but always in the same ballpark

### Memory

- `+` On eventual growth it produces very little garbage - about 50 times less than e. g. HashMap by total size of objects, that are going to be GC'ed.
- `+` Smaller memory footprint overall: only 45-55% of the size of `HashMap`

## Javolution's FastMap

`put`

- `-` Bad latency when hash table resize is triggered

## PauselessHashMap

- `-` Runs a background Executor with resizer threads; could lead to problems or stalls if resizer threads starve

### Computational

`put`

- `~` As good latency as `SmoothieMap`
- `+` Constant-time worst latencies

other operations

- remove(), putAll(), clear(), and the derivation of keysets and such will block for pending resize operations.
- Has amortized read and write performance close to `HashMap`'s, but is consistently slower

### Memory

- `~` Produces garbage on nearly the same rates as `HashMap`
- `~` Comparable footprint to `HashMap`

## Koloboke's `HashObjObjMap`

### Computational

- On average faster than `SmoothieMap`, but more variance

### Memory

- With `-XX:+UseCompressedOops`, on average smaller than `SmoothieMap`, but with greater variance
- With `-XX:-UseCompressedOops`, bigger than `SmoothieMap`
