from cbor2 import *
import pprint


def toParameterSet(paramSet):
    return map(lambda param: Parameter(param), paramSet)

class Parameter:
    def __init__(self, obj):

        self.key_name = obj['keyName']
        self.key_type = obj['keyType']

        if obj['keyType'] == 'StructKey':
            self.items = map(lambda item: toParameterSet(item['paramSet']), obj['items'])
        else: self.items = obj['items']

        self.units = obj['units']

    def __repr__(self):
        return "keyName="+self.key_name + \
           " keyType=" + self.key_type + \
           " items=" + str(self.items) + \
           " units=" + self.units

class Event:
    def __init__(self, obj):
        self.id = obj['eventId']
        self.prefix = obj['source']
        self.event_name = obj['eventName']
        self.event_time = obj['eventTime']
        self.param_set = toParameterSet(obj['paramSet'])

    def __repr__(self):
        return "id="+self.id + \
               " prefix=" + self.prefix +\
               " name=" + self.event_name +\
               " time=" + str(self.event_time) +\
               " paramSet=" + str(self.param_set)


with open('/tmp/input.cbor', 'rb') as fp:
    obj = load(fp)
    # pp = pprint.PrettyPrinter(indent=1, depth=5)
    # pp.pprint(obj)

    event = Event(obj)
    print "Decoded Event ======" + str(event)
