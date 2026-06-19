import re

with open('logcat500.txt', 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "WeatherWidget" in line or "Exception" in line:
        print(line.strip())
