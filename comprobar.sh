#!/bin/bash

clear

cat 0.log 1.log 2.log > total.log
sort -k 3 total.log > totalSorted.log
java Comprobador totalSorted.log $1 $2

