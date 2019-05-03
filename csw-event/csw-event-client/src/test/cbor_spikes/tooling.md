## Tools/Utilities helpful while debugging cbor encoding/decoding


#### For json representation of cbor

`cbor2json.rb /tmp/input.cbor`

#### For validating cbor against the schema

`cddl csw-event/csw-event-client/src/test/cbor_spikes/schema.cddl validate /tmp/input.cbor`

#### For generating sample jsons for your schema

This command helps while writing schema. It guides you with what all data can fit into the schema.

`cddl csw-event/csw-event-client/src/test/cbor_spikes/schema.cddl json-generate 5`

#### more tools 
Explore: https://github.com/cabo/cbor-diag