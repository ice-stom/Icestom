# LineFold
I wanted to be able to store line data for entire races without blowing up my disk space
- varint lz4 delta compression
- appears to cut 30% off the storage bill, designed for multithreaded stream writing
- 