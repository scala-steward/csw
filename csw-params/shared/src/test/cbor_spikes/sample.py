from cbor2 import *
import os.path
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
        return "Parameter(keyName="+self.key_name + \
           " keyType=" + self.key_type + \
           " items=" + str(self.items) + \
           " units=" + self.units + ")"

class Event:
    def __init__(self, obj):
        self.id = obj['eventId']
        self.prefix = obj['source']
        self.event_name = obj['eventName']
        self.event_time = obj['eventTime']
        self.param_set = toParameterSet(obj['paramSet'])

    def __repr__(self):
        return "id="+self.id + "\n" + \
               "prefix=" + self.prefix +"\n" + \
               "name=" + self.event_name +"\n" + \
               "time=" + str(self.event_time) +"\n" + \
               "paramSet=" + str(self.param_set)


my_path = os.path.abspath(os.path.dirname(__file__))
input_cbor_path = os.path.join(my_path, "input.cbor")

with open(input_cbor_path, 'rb') as fp:
    obj = load(fp)
    # pp = pprint.PrettyPrinter(indent=1, depth=5)
    # pp.pprint(obj)

    event = Event(obj)
    print "Decoded Event ======\n" + str(event)
