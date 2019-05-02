### CBOR Schema Doc

`items` in `parameter` could be oneof the following types:
* int
* float32
* float64 (double) 
* bool
* tstr
* bstr (byte string)
* radec 
* timestamp

// Array types
* [* int ]
* [* float32 ]
* [* float64 (double) ] 
* [* bool ]
* [* bstr (byte string) ]

// Matrix types
* [* [* int ] ]
* [* [* float32 ] ]
* [* [* float64 (double) ]  ] 
* [* [* bool ] ]
* [* [* bstr (byte string) ] ]