# COP-4520-Spring-2022-Programming-Assignment-3

You can compile and run prob1 by doing
javac prob1.java && java prob1

You might want to clean up by doing
rm *.class

You can compile and run prob2 by doing
g++ -std=c++20 -O3 -pthread prob2.cpp && time ./a.out 

If you don't have c++20, you might be able to get away with c++17

Prob 1 report:
We know it's wait free since there aren't any blocking methods in the program (except for the thread joins, but that's trivial).
It's correct since it's only slightly adapted from the implementation. 


Prob 2 report:
I represented 1 hour as 3 seconds (It would be really difficult to test otherwise!). You can change the constant ONE_MINUTE from 50ms 
to whatever you want, or you can leave it at 50ms (3 seconds / hour). It will run for 5 "hours" or 15 seconds. The proof of progress is
that it is completely wait-free; there is an atomic pointer to the front of an array where threads should insert their measurements into.
Since the readings are inserted with the time they are measured at, there is no issue when readings are inserted out of chronological order.
To gather the results, I just sweep over every measurement ever taken. I could have looping left and right pointers, and then have the new data
overwrite old data like how some queue implementations work, but it's not too hard to just have an array big enough. Inserting a new reading takes
constant time, simply incrementing and fetching an atomic integer and writing a pair in memory, and compiling the report, while linear in 
the number of datapoints recorded, only happens every so often, so it's fine, especially considering the extremely small number of datapoints
(~480 per window).