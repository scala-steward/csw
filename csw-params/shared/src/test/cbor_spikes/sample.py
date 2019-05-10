from cbor2 import *
import os.path
import pprint


def toParameterSet(paramSet):
    return map(lambda param: Parameter.from_dict(param), paramSet)


class Parameter:
    def __init__(self, key_name, key_type, items, units):
        self.key_name = key_name
        self.key_type = key_type
        self.items = items
        self.units = units

    @classmethod
    def from_dict(self, obj):
        key_name = obj['keyName']
        key_type = obj['keyType']
        if obj['keyType'] == 'StructKey':
            items = map(lambda item: toParameterSet(item['paramSet']), obj['items'])
        else:
            items = obj['items']

        units = obj['units']
        return Parameter(key_name, key_type, items, units)

    def to_dict(self):
        items = self.items if not self.key_type == "StructKey" else map(lambda item: item.to_dict, self.items)
        return {
            u"keyName": self.key_name,
            u"keyType": self.key_type,
            u"items": items,
            u"units": self.units
        }

    def __repr__(self):
        return "Parameter(keyName=" + self.key_name + \
               " keyType=" + self.key_type + \
               " items=" + str(self.items) + \
               " units=" + self.units + ")"


class Event:
    param_set = None
    id = None
    prefix = None
    event_name = None
    event_time = None

    @classmethod
    def from_dict(self, obj):
        id = obj['eventId']
        prefix = obj['source']
        event_name = obj['eventName']
        event_time = obj['eventTime']
        param_set = toParameterSet(obj['paramSet'])
        return Event(id, prefix, event_name, event_time, param_set)

    def to_dict(self):
        return [u"ObserveEvent", {
            u"eventId": self.id,
            u"source": self.prefix,
            u"eventName": self.event_name,
            u"eventTime": self.event_time,
            u"paramSet": map(lambda param: param.to_dict(), self.param_set)
        }]

    def __init__(self, id, prefix, event_name, event_time, param_set):
        self.id = id
        self.prefix = prefix
        self.event_name = event_name
        self.event_time = event_time
        self.param_set = param_set

    @classmethod
    def default_encoder(self, encoder, value):
        encoder.encode(self.to_dict(value))

    def __repr__(self):
        return "id=" + self.id + "\n" + \
               "prefix=" + self.prefix + "\n" + \
               "name=" + self.event_name + "\n" + \
               "time=" + str(self.event_time) + "\n" + \
               "paramSet=" + str(self.param_set)


my_path = os.path.abspath(os.path.dirname(__file__))
input_cbor_path = os.path.join(my_path, "sample_encoded_data/event.cbor")

e = Event(u"id", u"prefix", u"eventname", {u"nanos": 403, u"seconds": 15},
          set([Parameter(u"ints", u"IntKey", [0], u"NoUnits")]))

with open(input_cbor_path, 'wb') as fp:
    dump(e, fp, default=Event.default_encoder)

with open(input_cbor_path, 'rb') as fp:
    [type, obj] = load(fp)
    print Event.from_dict(obj)
