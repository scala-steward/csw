from cbor2 import *

class Blob:
    def __init__(self, bytes, ints, floats, doubles, strings, innerObjects):
        self.bytes = bytes
        self.ints = ints
        self.floats = floats
        self.doubles = doubles
        self.strings = strings
        self.innerObjects = innerObjects


with open('/tmp/input.cbor', 'rb') as fp:
    obj = load(fp)
    print "Obj=" + str(obj)
    blob = Blob(*obj)
    print "blob=" + str(blob)
