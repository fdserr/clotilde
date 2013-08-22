# clotilde

<img src="https://github.com/justiniac/clotilde/blob/master/resources/clotilde.jpg?raw=true"
 alt="Clotilde" title="Clotilde, first queen of France" align="right" hspace="16"/>
<!---
TODO: idiomatic md link ?
--->

Clotilde is a port of the Linda process coordination language, in an attempt at idiomatic Clojure.
>"In computer science, Linda is a model of coordination and communication among several 
>parallel processes operating upon objects stored in and retrieved from shared, virtual, 
>associative memory. Linda was developed by David Gelernter and Nicholas Carriero at 
>Yale University and is named for Linda Lovelace, an actress in the porn movie Deep Throat, 
>a pun on Ada's tribute to Ada Lovelace."
>    <a href="http://en.wikipedia.org/wiki/Linda_(coordination_language)">- Wikipedia -</a>

Linda is a registered trademark of SCIENTIFIC Computing Associates, Inc.

Clotilde ( ~475-545* ) is the registered spouse of Clovis, and therefore is known to be the first ever queen of France. 
(*) Yes, 70 years old can be regarded as tough considering the era, and also her job, and especially the hubby. 
<!---
TODO: proper credit WP
--->

Back to Linda... In order to get anything close to useful done with the language's four instructions 
(indeed 4: out, eval, rd, in), one needs some acquaintance 
with a small bunch of sparsely taught concepts such as tuple spaces or associative virtual memory, 
live data structures, and more generally "the game of parallelism". Fortunately, it's a matter of a couple of hours reading:
HOW TO WRITE PARALLEL PROGRAMS: A FIRST COURSE
, By Nicholas Carriero and David Gelernter
_©1990 Massachusetts Institute of Technology
, ISBN 0-262-03171-X_.
Well worth a read for anyone willing to program for more than a handful of cores in any language,
especially when the book is short and unpedantic, and when it's
[freely available for browsing online](http://www.lindaspaces.com/book/), 
and when it can also be [downloaded in pdf format](http://www.lindaspaces.com/book/book.pdf). 
Great times we live in; if only Clotilde had html and pdf, she'd probably have stayed for one more century or two.
For deeper litterature on the topic, see [Science Direct](http://www.sciencedirect.com/science/article/pii/S0890540199928237)
(no, I havn't read all the references, and I won't, but some of these papers are really worth the time).

## Credits

David Gelernter, Joe Armstrong, Rich Hickey: thank you for all those nights of mine you ruined.

Drew Colthorp: mucho thankies for all the nights of mine you saved; illumination! (matchure/fn-match).

## Usage

Add a Leiningen depency to your project.clj:
```clojure
(defproject your-project "0.0.1"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clotilde "0.2.1-SNAPSHOT"]])
````

Add a :use clause to your namespace:

```clojure
(ns your-project.core
  (:use clotilde.core    ;; API
        clotilde.tools   ;; utilities
        ))
````

For usage and instructions, see the program's documentation.

```clojure
;; TODO: write documentation.
````

Kidding, the code is documented, and comes with tests; idiomatic Clojure ain't it?

Have fun.

## License

Copyright (c) 2013 François De Serres, _aka._ Justiniac 

Distributed under the Eclipse Public License, the same as Clojure.
