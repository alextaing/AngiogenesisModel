import os
import csv
import re
from statistics import mean

export_data = True
dataset = input("What dataset would you like to use?\n")

path = os.path.join("EndoData", dataset)

first_arrival_time = {}
average_arrival_times = {}
average_arrival_length = {}
average_invasion_depth = {}
average_final_length = {}
total_anastomoses = {}
average_vessel_density = {}
inner_50_density = {}

for trial in os.scandir(path):
	if trial.path.endswith(".csv"):
		with open(trial.path) as csv_file:
			csv_reader = csv.reader(csv_file, delimiter=",")
			print("\n")
			print(trial.path)

			percent_searcher = re.compile('\d+\.\d+%')
			percent = percent_searcher.search(str(trial.path)).group()
			print(percent)

			for row in csv_reader:
				dataTitle = ""
				try:
					dataTitle = row[0]
				except:
					print("DID NOT ARRIVE")

					datapoint = 0

				if dataTitle == "Arrival Time (h)":
					datapoint = min(row[1:])
					print(f"First Arrival Time: {datapoint}")
					if datapoint is not None:
						if percent not in first_arrival_time.keys():
							first_arrival_time[percent] = [datapoint]
						elif percent in first_arrival_time.keys():
							first_arrival_time[percent].append(datapoint)


					datapoint2 = mean([float(data) for data in row[1:]])
					print(f"Average Arrival Time: {datapoint}")
					if datapoint is not None:
						if percent not in average_arrival_times.keys():
							average_arrival_times[percent] = [datapoint]
						elif percent in average_arrival_times.keys():
							average_arrival_times[percent].append(datapoint)

				if dataTitle == "Length at arrival (microns)":
					datapoint = mean([float(data) for data in row[1:]])
					if datapoint is not None:
						if percent not in average_arrival_length.keys():
							average_arrival_length[percent] = [datapoint]
						elif percent in average_arrival_length.keys():
							average_arrival_length[percent].append(datapoint)
					print(f"Average Arrival Length: {datapoint}")

				if dataTitle == "Invasion depth (microns)":
					datapoint = mean([float(data) for data in row[1:]])
					if datapoint is not None:
						if percent not in average_invasion_depth.keys():
							average_invasion_depth[percent] = [datapoint]
						elif percent in average_invasion_depth.keys():
							average_invasion_depth[percent].append(datapoint)
					print(f"Average Invasion Depth: {datapoint}")

				if dataTitle == "Final Length (microns)":
					datapoint = mean([float(data) for data in row[1:]])
					if datapoint is not None:
						if percent not in average_final_length.keys():
							average_final_length[percent] = [datapoint]
						elif percent in average_final_length.keys():
							average_final_length[percent].append(datapoint)
					print(f"Average Final Length: {datapoint}")

				if dataTitle == "Total Anastomoses":
					datapoint = int(row[1])
					if datapoint is not None:
						if percent not in total_anastomoses.keys():
							total_anastomoses[percent] = [datapoint]
						elif percent in total_anastomoses.keys():
							total_anastomoses[percent].append(datapoint)
					print(f"Total Anastomoses: {datapoint}")

				if dataTitle == "Average Vessel Density":
					datapoint = float(row[1][0:-1])
					if datapoint is not None:
						if percent not in average_vessel_density.keys():
							average_vessel_density[percent] = [datapoint]
						elif percent in average_vessel_density.keys():
							average_vessel_density[percent].append(datapoint)
					print(f"Average Vessel Density: {datapoint}")

				if dataTitle == "Inner 50% Vessel Density":
					datapoint = float(row[1][0:-1])
					if datapoint is not None:
						if percent not in inner_50_density.keys():
							inner_50_density[percent] = [datapoint]
						elif percent in inner_50_density.keys():
							inner_50_density[percent].append(datapoint)
					print(f"Inner 50% Vessel Density: {datapoint}")

print("\n\n\n\n")


# AVERAGES

Dicts = [first_arrival_time, average_arrival_times, average_arrival_length, average_invasion_depth, average_final_length, total_anastomoses, average_vessel_density, inner_50_density]

for dictionary in Dicts:
	for values in dictionary.values():
		average = mean([float(value) for value in values])
		values.append(f"AVG: {average}")
# COMPLETE CSV

csv = ""

csv = "First Arrival Time (h)\n"
for trial in first_arrival_time.keys():
	csv += f"{trial}"
	for point in first_arrival_time[trial]:
		csv += f",{point}"
	csv += "\n"

csv += "Average Arrival Times (h)\n"
for trial in average_arrival_times.keys():
	csv += f"{trial}"
	for point in average_arrival_times[trial]:
		csv += f",{point}"
	csv += "\n"

csv += "Average Arrival Lengths (microns)\n"
for trial in average_arrival_length.keys():
	csv += f"{trial}"
	for point in average_final_length[trial]:
		csv += f",{point}"
	csv += "\n"

csv += "Average Invasion Depth (microns)\n"
for trial in average_invasion_depth.keys():
	csv += f"{trial}"
	for point in average_invasion_depth[trial]:
		csv += f",{point}"
	csv += "\n"


csv += "Average Final Lengths (microns)\n"
for trial in average_arrival_length.keys():
	csv += f"{trial}"
	for point in average_arrival_length[trial]:
		csv += f",{point}"
	csv += "\n"

csv += "Total Anastomoses\n"
for trial in total_anastomoses.keys():
	csv += f"{trial}"
	for point in total_anastomoses[trial]:
		csv += f",{point}"
	csv += "\n"

csv += "Average Vessel Density\n"
for trial in average_vessel_density.keys():
	csv += f"{trial}"
	for point in average_vessel_density[trial]:
		csv += f",{point}"
	csv += "\n"

csv += "Inner 50% Density\n"
for trial in inner_50_density.keys():
	csv += f"{trial}"
	for point in inner_50_density[trial]:
		csv += f",{point}"
	csv += "\n"


print(csv)

if export_data:
	f = open(f"{path}\\DataSummary.csv", "w")
	print(csv, file=f)
	f.close

