import json
import sys

f = sys.argv[1]

json_file = open(f)

json_data = json.load(json_file)

for elt in json_data["functions"]:
    print(f"{elt['name']}:{elt['startAddr']}")
