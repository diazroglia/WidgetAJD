import urllib.request
import json
import urllib.parse

# List of fields we want to check
fields = [
    "temperature",
    "temperatureApparent",
    "dewPoint",
    "humidity",
    "windSpeed",
    "windGust",
    "windDirection",
    "precipitationIntensity",
    "precipitationType",
    "visibility",
    "cloudCover",
    "uvIndex",
    "weatherCode"
]

fields_str = ",".join(fields)
location = "-34.9011,-56.1645" # Montevideo
apikey = "bVnE3WhwiBzYwhEmqRlwWnKmlJIEZBe3"

url = f"https://api.tomorrow.io/v4/timelines?location={location}&fields={fields_str}&timesteps=current&apikey={apikey}"

req = urllib.request.Request(url, headers={'Accept': 'application/json'})
try:
    with urllib.request.urlopen(req) as response:
        res_data = response.read().decode()
        data = json.loads(res_data)
        print(json.dumps(data, indent=2))
except Exception as e:
    print(f"Error querying API: {e}")
