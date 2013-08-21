# Introduction to clotilde

## initialize! [] / [vector-of-vectors]

-> nil

side effect: blank space / space as vector-of-vectors, empty queues.

## out! [& exprs] 

-> vector

side effect: vector added to space.

## eval! [& exprs] 

-> (future vector)

side effect: vector added to space by another thread.

## rd! [patterns & exprs] 
-> exprs

with bindings set using patterns, side effect: may queue up and block until matching.

## in! [patterns & exprs] 
-> exprs

with bindings set using patterns, side effect: may queue up and block until matching, 
remove a matching vector form space.


